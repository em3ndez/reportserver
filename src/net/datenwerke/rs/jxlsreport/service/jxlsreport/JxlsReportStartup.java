package net.datenwerke.rs.jxlsreport.service.jxlsreport;

import org.jxls.builder.xls.XlsCommentAreaBuilder;

import com.google.inject.Inject;
import com.google.inject.Provider;

import net.datenwerke.gf.service.lateinit.LateInitHook;
import net.datenwerke.gf.service.upload.hooks.FileUploadHandlerHook;
import net.datenwerke.hookhandler.shared.hookhandler.HookHandlerService;
import net.datenwerke.rs.base.service.hooks.UsageStatisticsReportEntryProviderHook;
import net.datenwerke.rs.core.service.reportmanager.hookers.factory.ReportDefaultMergeHookerFactory;
import net.datenwerke.rs.core.service.reportmanager.hooks.ReportEngineProviderHook;
import net.datenwerke.rs.core.service.reportmanager.hooks.ReportTypeProviderHook;
import net.datenwerke.rs.jxlsreport.service.jxlsreport.entities.JxlsReport;
import net.datenwerke.rs.jxlsreport.service.jxlsreport.hookers.BaseJxlsOutputGeneratorProvider;
import net.datenwerke.rs.jxlsreport.service.jxlsreport.hookers.JxlsReportEngineProviderHooker;
import net.datenwerke.rs.jxlsreport.service.jxlsreport.hookers.JxlsReportTypeProviderHooker;
import net.datenwerke.rs.jxlsreport.service.jxlsreport.hookers.JxlsReportUploadHooker;
import net.datenwerke.rs.jxlsreport.service.jxlsreport.hookers.UsageStatisticsJxlsProviderHooker;
import net.datenwerke.rs.jxlsreport.service.jxlsreport.reportengine.hooks.JxlsOutputGeneratorProviderHook;
import net.datenwerke.rs.utils.entitymerge.service.hooks.EntityMergeHook;

public class JxlsReportStartup {

   @Inject
   public JxlsReportStartup(
         final HookHandlerService hookHandlerService,
         final JxlsReportEngineProviderHooker jxlsReportEngineProviderHooker,
         final Provider<BaseJxlsOutputGeneratorProvider> baseOutputGenerators,
         final JxlsReportUploadHooker jxlsReportUploadHooker,
         
         final Provider<UsageStatisticsJxlsProviderHooker> usageStatsJxlsProvider,
         final Provider<ReportDefaultMergeHookerFactory> reportFactory
         ) {

      hookHandlerService.attachHooker(ReportTypeProviderHook.class, new JxlsReportTypeProviderHooker());
      hookHandlerService.attachHooker(ReportEngineProviderHook.class, jxlsReportEngineProviderHooker);
      hookHandlerService.attachHooker(FileUploadHandlerHook.class, jxlsReportUploadHooker);

      /* base exporters */
      hookHandlerService.attachHooker(JxlsOutputGeneratorProviderHook.class, baseOutputGenerators,
            HookHandlerService.PRIORITY_LOW);

      hookHandlerService.attachHooker(LateInitHook.class, () -> {
         // multi-line and comment support
         XlsCommentAreaBuilder.MULTI_LINE_SQL_FEATURE = true;
         
         // collapsible rows support
         XlsCommentAreaBuilder.addCommandMapping("groupRow", GroupRowCommand.class);
      });
      
      /* entity merge */
      hookHandlerService.attachHooker(EntityMergeHook.class, reportFactory.get().create(JxlsReport.class));
      

      hookHandlerService.attachHooker(UsageStatisticsReportEntryProviderHook.class, usageStatsJxlsProvider,
            HookHandlerService.PRIORITY_LOW + 25);
   }

}
