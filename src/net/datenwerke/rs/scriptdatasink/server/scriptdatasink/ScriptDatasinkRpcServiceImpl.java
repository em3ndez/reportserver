package net.datenwerke.rs.scriptdatasink.server.scriptdatasink;

import static net.datenwerke.rs.utils.exception.shared.LambdaExceptionUtil.rethrowFunction;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;

import net.datenwerke.gxtdto.client.servercommunication.exceptions.ExpectedException;
import net.datenwerke.gxtdto.client.servercommunication.exceptions.ServerCallFailedException;
import net.datenwerke.gxtdto.server.dtomanager.DtoService;
import net.datenwerke.hookhandler.shared.hookhandler.HookHandlerService;
import net.datenwerke.rs.core.client.datasinkmanager.DatasinkTestFailedException;
import net.datenwerke.rs.core.client.datasinkmanager.dto.DatasinkDefinitionDto;
import net.datenwerke.rs.core.client.reportexporter.dto.ReportExecutionConfigDto;
import net.datenwerke.rs.core.client.reportmanager.dto.reports.ReportDto;
import net.datenwerke.rs.core.server.reportexport.hooks.ReportExportViaSessionHook;
import net.datenwerke.rs.core.service.datasinkmanager.DatasinkService;
import net.datenwerke.rs.core.service.datasinkmanager.configs.DatasinkConfiguration;
import net.datenwerke.rs.core.service.datasinkmanager.configs.DatasinkFilenameConfig;
import net.datenwerke.rs.core.service.datasinkmanager.entities.DatasinkDefinition;
import net.datenwerke.rs.core.service.datasinkmanager.hooks.DatasinkDispatchNotificationHook;
import net.datenwerke.rs.core.service.reportmanager.ReportDtoService;
import net.datenwerke.rs.core.service.reportmanager.ReportExecutorService;
import net.datenwerke.rs.core.service.reportmanager.ReportService;
import net.datenwerke.rs.core.service.reportmanager.engine.CompiledReport;
import net.datenwerke.rs.core.service.reportmanager.engine.config.RECReportExecutorToken;
import net.datenwerke.rs.core.service.reportmanager.engine.config.ReportExecutionConfig;
import net.datenwerke.rs.core.service.reportmanager.entities.reports.Report;
import net.datenwerke.rs.fileserver.client.fileserver.dto.AbstractFileServerNodeDto;
import net.datenwerke.rs.scheduleasfile.client.scheduleasfile.StorageType;
import net.datenwerke.rs.scriptdatasink.client.scriptdatasink.dto.ScriptDatasinkDto;
import net.datenwerke.rs.scriptdatasink.client.scriptdatasink.rpc.ScriptDatasinkRpcService;
import net.datenwerke.rs.scriptdatasink.service.scriptdatasink.ScriptDatasinkService;
import net.datenwerke.rs.scriptdatasink.service.scriptdatasink.definitions.ScriptDatasink;
import net.datenwerke.rs.utils.exception.ExceptionService;
import net.datenwerke.rs.utils.zip.ZipUtilsService;
import net.datenwerke.security.server.SecuredRemoteServiceServlet;
import net.datenwerke.security.service.security.SecurityService;
import net.datenwerke.security.service.security.rights.Execute;
import net.datenwerke.security.service.security.rights.Read;

@Singleton
public class ScriptDatasinkRpcServiceImpl extends SecuredRemoteServiceServlet implements ScriptDatasinkRpcService {

   /**
    * 
    */
   private static final long serialVersionUID = 472043191468008208L;

   private final ReportService reportService;
   private final DtoService dtoService;
   private final ReportExecutorService reportExecutorService;
   private final ReportDtoService reportDtoService;
   private final HookHandlerService hookHandlerService;
   private final ScriptDatasinkService scriptDatasinkService;
   private final SecurityService securityService;
   private final ExceptionService exceptionServices;
   private final ZipUtilsService zipUtilsService;
   private final Provider<DatasinkService> datasinkServiceProvider;

   @Inject
   public ScriptDatasinkRpcServiceImpl(
         ReportService reportService, 
         ReportDtoService reportDtoService,
         DtoService dtoService, 
         ReportExecutorService reportExecutorService, 
         SecurityService securityService,
         HookHandlerService hookHandlerService, 
         ScriptDatasinkService scriptDatasinkService,
         ExceptionService exceptionServices, 
         ZipUtilsService zipUtilsService,
         Provider<DatasinkService> datasinkServiceProvider
         ) {

      this.reportService = reportService;
      this.reportDtoService = reportDtoService;
      this.dtoService = dtoService;
      this.reportExecutorService = reportExecutorService;
      this.securityService = securityService;
      this.hookHandlerService = hookHandlerService;
      this.scriptDatasinkService = scriptDatasinkService;
      this.exceptionServices = exceptionServices;
      this.zipUtilsService = zipUtilsService;
      this.datasinkServiceProvider = datasinkServiceProvider;
   }

   @Transactional(rollbackOn = { Exception.class })
   @Override
   public void exportReportIntoDatasink(ReportDto reportDto, String executorToken, DatasinkDefinitionDto datasinkDto,
         String format, List<ReportExecutionConfigDto> configs, String name, boolean compressed)
         throws ServerCallFailedException {
      if (!(datasinkDto instanceof ScriptDatasinkDto))
         throw new IllegalArgumentException("Not a script datasink");

      final ReportExecutionConfig[] configArray = getConfigArray(executorToken, configs);

      ScriptDatasink scriptDatasink = (ScriptDatasink) dtoService.loadPoso(datasinkDto);

      /* get a clean and unmanaged report from the database */
      Report referenceReport = reportDtoService.getReferenceReport(reportDto);
      Report orgReport = (Report) reportService.getUnmanagedReportById(reportDto.getId());

      /* check rights */
      securityService.assertRights(referenceReport, Execute.class);
      securityService.assertRights(scriptDatasink, Read.class, Execute.class);

      /* create variant */
      Report adjustedReport = (Report) dtoService.createUnmanagedPoso(reportDto);
      final Report toExecute = orgReport.createTemporaryVariant(adjustedReport);

      hookHandlerService.getHookers(ReportExportViaSessionHook.class)
            .forEach(hooker -> hooker.adjustReport(toExecute, configArray));

      CompiledReport cReport;
      try {
         cReport = reportExecutorService.execute(toExecute, format, configArray);
         
         final DatasinkConfiguration config = new DatasinkFilenameConfig() {
            @Override
            public String getFilename() {
               return datasinkServiceProvider.get().getFilenameForDatasink(name, cReport, compressed);
            }
         };
         datasinkServiceProvider.get().exportIntoDatasink(cReport.getReport(), scriptDatasink, config);
         hookHandlerService.getHookers(DatasinkDispatchNotificationHook.class).forEach(
               hooker -> hooker.notifyOfCompiledReportDispatched(cReport.getReport(), scriptDatasink, config));
         
      } catch (Exception e) {
         throw new ServerCallFailedException("Could not send report to script datasink: " + e.getMessage(), e);
      }

   }

   private ReportExecutionConfig[] getConfigArray(final String executorToken,
         final List<ReportExecutionConfigDto> configs) throws ExpectedException {
      return Stream.concat(
            configs.stream().map(rethrowFunction(config -> (ReportExecutionConfig) dtoService.createPoso(config))),
            Stream.of(new RECReportExecutorToken(executorToken))).toArray(ReportExecutionConfig[]::new);
   }

   @Override
   public Map<StorageType, Boolean> getStorageEnabledConfigs() throws ServerCallFailedException {
      return datasinkServiceProvider.get().getEnabledConfigs(scriptDatasinkService);
   }

   @Override
   public boolean testScriptDatasink(ScriptDatasinkDto scriptDatasinkDto)
         throws ServerCallFailedException {
      ScriptDatasink scriptDatasink = (ScriptDatasink) dtoService
            .loadPoso(scriptDatasinkDto);

      /* check rights */
      securityService.assertRights(scriptDatasink, Read.class, Execute.class);

      try {
         datasinkServiceProvider.get().testDatasink(scriptDatasink,
               new DatasinkFilenameConfig() {
                  @Override
                  public String getFilename() {
                     return "reportserver-script-datasink-test.txt";
                  }
               });
      } catch (Exception e) {
         DatasinkTestFailedException ex = new DatasinkTestFailedException(ExceptionUtils.getRootCauseMessage(e), e);
         ex.setStackTraceAsString(exceptionServices.exceptionToString(e));
         throw ex;
      }

      return true;
   }

   @Override
   public DatasinkDefinitionDto getDefaultDatasink() throws ServerCallFailedException {

      Optional<? extends DatasinkDefinition> defaultDatasink = datasinkServiceProvider.get()
            .getDefaultDatasink(scriptDatasinkService);
      if (!defaultDatasink.isPresent())
         return null;

      /* check rights */
      securityService.assertRights(defaultDatasink.get(), Read.class);

      return (DatasinkDefinitionDto) dtoService.createDto(defaultDatasink.get());
   }

   @Override
   public void exportFileIntoDatasink(AbstractFileServerNodeDto abstractNodeDto, DatasinkDefinitionDto datasinkDto,
         String filename, boolean compressed) throws ServerCallFailedException {
      /* check rights */
      securityService.assertRights(abstractNodeDto, Read.class);
      securityService.assertRights(datasinkDto, Read.class, Execute.class);
      datasinkServiceProvider.get().exportFileIntoDatasink(abstractNodeDto, datasinkDto,
            filename, null, compressed);
   }

}