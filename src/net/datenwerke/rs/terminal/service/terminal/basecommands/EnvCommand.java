package net.datenwerke.rs.terminal.service.terminal.basecommands;

import static java.util.stream.Collectors.joining;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.dialect.Dialect;

import com.google.inject.Inject;
import com.google.inject.Provider;

import net.datenwerke.rs.EnvironmentValidator;
import net.datenwerke.rs.adminutils.service.systemconsole.generalinfo.GeneralInfoService;
import net.datenwerke.rs.base.service.datasources.DatasourceHelperService;
import net.datenwerke.rs.base.service.datasources.definitions.DatabaseDatasource;
import net.datenwerke.rs.base.service.reportengines.table.output.object.RSStringTableRow;
import net.datenwerke.rs.base.service.reportengines.table.output.object.RSTableModel;
import net.datenwerke.rs.base.service.reportengines.table.output.object.TableDefinition;
import net.datenwerke.rs.core.service.internaldb.TempTableService;
import net.datenwerke.rs.terminal.service.terminal.TerminalSession;
import net.datenwerke.rs.terminal.service.terminal.exceptions.TerminalException;
import net.datenwerke.rs.terminal.service.terminal.helpers.AutocompleteHelper;
import net.datenwerke.rs.terminal.service.terminal.helpers.CommandParser;
import net.datenwerke.rs.terminal.service.terminal.helpmessenger.annotations.CliHelpMessage;
import net.datenwerke.rs.terminal.service.terminal.hooks.TerminalCommandHook;
import net.datenwerke.rs.terminal.service.terminal.locale.TerminalMessages;
import net.datenwerke.rs.terminal.service.terminal.obj.CommandResult;

public class EnvCommand implements TerminalCommandHook {

   public static final String BASE_COMMAND = "env";
   private final Provider<GeneralInfoService> generalInfoServiceProvider;
   private final Provider<DatasourceHelperService> datasourceHelperServiceProvider;
   private final Provider<TempTableService> tempTableServiceProvider;

   @Inject
   public EnvCommand(
         Provider<GeneralInfoService> generalInfoServiceProvider, 
         Provider<DatasourceHelperService> datasourceHelperServiceProvider,
         Provider<TempTableService> tempTableServiceProvider
         ) {
      this.generalInfoServiceProvider = generalInfoServiceProvider;
      this.datasourceHelperServiceProvider = datasourceHelperServiceProvider;
      this.tempTableServiceProvider = tempTableServiceProvider;
   }

   @Override
   public boolean consumes(CommandParser parser, TerminalSession session) {
      return BASE_COMMAND.equals(parser.getBaseCommand());
   }

   @Override
   @CliHelpMessage(
         messageClass = TerminalMessages.class, 
         name = BASE_COMMAND, 
         description = "commandEnv_description"
         )
   public CommandResult execute(CommandParser parser, TerminalSession session) throws TerminalException {
      CommandResult result = new CommandResult();

      GeneralInfoService generalInfoService = generalInfoServiceProvider.get();

      RSTableModel table = new RSTableModel();
      TableDefinition td = new TableDefinition(Arrays.asList("General Info", ""),
            Arrays.asList(String.class, String.class));
      td.setDisplaySizes(Arrays.asList(100, 0));
      table.setTableDefinition(td);

      Runtime runtime = Runtime.getRuntime();
      int mb = 1024 * 1024;
      
      table.addDataRow(new RSStringTableRow("Version", generalInfoService.getRsVersion()));
      table.addDataRow(new RSStringTableRow("Java version", generalInfoService.getJavaVersion()));
      table.addDataRow(new RSStringTableRow("JVM Args", generalInfoService.getVmArguments()));
      table.addDataRow(new RSStringTableRow("Application server", generalInfoService.getApplicationServer()));
      table.addDataRow(new RSStringTableRow("Max memory",
            NumberFormat.getIntegerInstance().format(runtime.maxMemory() / mb) + " MB"));
      table.addDataRow(new RSStringTableRow("Groovy Version", generalInfoService.getGroovyVersion()));
      table.addDataRow(new RSStringTableRow("Locale", generalInfoService.getLocale()));
      table.addDataRow(new RSStringTableRow("JVM Locale", generalInfoService.getJvmLocale()));
      table.addDataRow(new RSStringTableRow("Operation system", generalInfoService.getOsVersion()));
      table.addDataRow(new RSStringTableRow("User Agent", generalInfoService.getUserAgent()));

      result.addResultTable(table);
      try {
         result.addResultTable(getPamsAsTable(generalInfoService));
         result.addResultTable(getDbConfigAsTable());
         result.addResultTable(getInternalDbInformationAsTable());
         result.addResultTable(getSslInformationAsTable(generalInfoService));
      } catch (SQLException e) {
         throw new TerminalException(e);
      }
      return result;
   }

   @Override
   public void addAutoCompletEntries(AutocompleteHelper autocompleteHelper, TerminalSession session) {
      autocompleteHelper.autocompleteBaseCommand(BASE_COMMAND);
   }
   
   private RSTableModel getSslInformationAsTable(GeneralInfoService generalInfoService) throws SQLException {
      RSTableModel table = new RSTableModel();
      TableDefinition td = new TableDefinition(Arrays.asList("SSL", ""),
            Arrays.asList(String.class, String.class));
      table.setTableDefinition(td);
      td.setDisplaySizes(Arrays.asList(100, 0));

      table.addDataRow(new RSStringTableRow("Supported SSL protocols",
            generalInfoService.getSupportedSslProtocols().stream().collect(joining(", "))));
      table.addDataRow(new RSStringTableRow("Default SSL protocols",
            generalInfoService.getDefaultSslProtocols().stream().collect(joining(", "))));
      table.addDataRow(new RSStringTableRow("Enabled SSL protocols",
            generalInfoService.getEnabledSslProtocols().stream().collect(joining(", "))));

      return table;
   }
   
   private RSTableModel getPamsAsTable(GeneralInfoService generalInfoService) throws SQLException {
      final RSTableModel table = new RSTableModel();
      final TableDefinition td = new TableDefinition(Arrays.asList("Static PAM Configuration"),
            Arrays.asList(String.class));
      table.setTableDefinition(td);
      
      final List<String> staticPams = generalInfoService.getStaticPams();
      if (staticPams.isEmpty()) {
         table.addDataRow(new RSStringTableRow("No static PAM configured"));
         return table;
      }
      
      staticPams.forEach(pam -> table.addDataRow(new RSStringTableRow(pam)));
      
      return table;
   }
   
   private RSTableModel getDbConfigAsTable() throws SQLException {
      RSTableModel table = new RSTableModel();
      TableDefinition td = new TableDefinition(Arrays.asList("DB Config", ""),
            Arrays.asList(String.class, String.class));
      table.setTableDefinition(td);
      td.setDisplaySizes(Arrays.asList(100, 0));      
      
      Properties jpaProperties = EnvironmentValidator.getJpaProperties();
      table.addDataRow(new RSStringTableRow("hibernate.dialect", jpaProperties.getProperty("hibernate.dialect")));
      table.addDataRow(new RSStringTableRow("hibernate.connection.driver_class", jpaProperties.getProperty("hibernate.connection.driver_class")));
      table.addDataRow(new RSStringTableRow("hibernate.connection.url", jpaProperties.getProperty("hibernate.connection.url")));
      table.addDataRow(new RSStringTableRow("hibernate.connection.username", jpaProperties.getProperty("hibernate.connection.username")));
      table.addDataRow(new RSStringTableRow("hibernate.default_schema", "hibernate.default_schema"));

      Class<?> dialectClass = null;
      try {
         dialectClass = Class.forName(jpaProperties.getProperty("hibernate.dialect"));
      } catch (ClassNotFoundException e1) {
      }
      Connection conn = null;
      try {
         Dialect dialect = (Dialect) dialectClass.newInstance();
         String query = dialect.getCurrentTimestampSelectString();
         conn = EnvironmentValidator.openConnection();
         PreparedStatement stmt = conn.prepareStatement(query);
         ResultSet resultSet = stmt.executeQuery();
         resultSet.close();
         stmt.close();
         table.addDataRow(new RSStringTableRow("Connection Test", "OK"));
         try {
            stmt = conn.prepareStatement(
                  "SELECT * FROM RS_SCHEMAINFO WHERE KEY_FIELD = 'schemaversion' ORDER BY ENTITY_ID DESC");
            resultSet = stmt.executeQuery();
            if (resultSet.next()) {
               table.addDataRow(new RSStringTableRow("Schema Version", resultSet.getString("value")));
            } else {
               table.addDataRow(new RSStringTableRow("Schema Version", "No version number found. Did you forget a commit during installation?"));
            }
         } catch (SQLException e) {
            table.addDataRow(new RSStringTableRow("Schema Version", "Unknown (" + e.getMessage() + ")"));
         } finally {
            resultSet.close();
            stmt.close();
         }
      }
      catch (Exception e) {
         table.addDataRow(new RSStringTableRow("Connection Test", "Failed (" + e.getMessage() + ")"));
      } finally {
         if (null != conn) {
            try {
               conn.close();
            } catch (SQLException e) {
               table.addDataRow(new RSStringTableRow("Connection Test", "Failed (" + e.getMessage() + ")"));
            }
         }
      }
      
      return table;
   }
   
   private RSTableModel getInternalDbInformationAsTable() throws SQLException {
      RSTableModel table = new RSTableModel();
      TableDefinition td = new TableDefinition(Arrays.asList("Internal datasource info", ""),
            Arrays.asList(String.class, String.class));
      table.setTableDefinition(td);
      td.setDisplaySizes(Arrays.asList(100, 0));
      
      DatabaseDatasource internalDbDatasource = tempTableServiceProvider.get().getInternalDbDatasource();
      if (null == internalDbDatasource) {
         table.addDataRow(new RSStringTableRow(
               "No internal database found. Check your /fileserver/etc/datasources/internaldb.cf configuration file.",
               ""));
         return table;
      }
      
      DatasourceHelperService datasourceHelperService = datasourceHelperServiceProvider.get();
      
      Map<String, Object> datasourceInfoDefinition = datasourceHelperService.getDatasourceInfoDefinition();
      Map<String, String> generalInfo = (Map<String, String>) datasourceInfoDefinition.get("generalInfo");
      Map<String, String> urlInfo = (Map<String, String>) datasourceInfoDefinition.get("urlInfo");
      Map<String, Object> datasourceMetadata = datasourceHelperService.fetchInfoDatasourceMetadata(internalDbDatasource);
      
      table.addDataRow(new RSStringTableRow("ID", internalDbDatasource.getId()+""));
      table.addDataRow(new RSStringTableRow("Name", internalDbDatasource.getName()));
      addInternalDbInfoToTable(table, generalInfo, datasourceMetadata);
      addInternalDbInfoToTable(table, urlInfo, datasourceMetadata);
      
      return table;
   }
   
   private void addInternalDbInfoToTable(RSTableModel table, Map<String, String> infoDefinition,
         Map<String, Object> datasourceMetadata) {
      infoDefinition.forEach(
            (methodDesc, methodName) -> {
               String result = datasourceMetadata.get(methodName).toString();
               table.addDataRow(new RSStringTableRow(methodDesc, result));
            });
   }
}
