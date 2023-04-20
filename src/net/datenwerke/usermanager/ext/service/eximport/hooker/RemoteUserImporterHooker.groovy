package net.datenwerke.usermanager.ext.service.eximport.hooker;

import static net.datenwerke.rs.base.ext.service.RemoteEntityImporterServiceImpl.handleError

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.google.inject.Provider

import net.datenwerke.eximport.ExportDataAnalyzerServiceImpl
import net.datenwerke.eximport.ImportService
import net.datenwerke.eximport.im.ImportConfig
import net.datenwerke.eximport.im.ImportResult
import net.datenwerke.rs.base.ext.service.RemoteEntityImports
import net.datenwerke.rs.base.ext.service.hooks.RemoteEntityImporterHook
import net.datenwerke.security.service.usermanager.UserManagerService
import net.datenwerke.security.service.usermanager.entities.OrganisationalUnit
import net.datenwerke.treedb.ext.service.eximport.TreeNodeImporterConfig
import net.datenwerke.treedb.service.treedb.AbstractNode
import net.datenwerke.usermanager.ext.service.eximport.UserManagerExporter

class RemoteUserImporterHooker implements RemoteEntityImporterHook {

   private final Provider<ExportDataAnalyzerServiceImpl> analyzerServiceProvider
   private final Provider<UserManagerService> userManagerServiceProvider
   private final Provider<ImportService> importServiceProvider
   
   private final Logger logger = LoggerFactory.getLogger(getClass().name)
   
   @Inject
   public RemoteUserImporterHooker(
      Provider<ExportDataAnalyzerServiceImpl> analyzerServiceProvider,
      Provider<UserManagerService> userManagerServiceProvider,
      Provider<ImportService> importServiceProvider
      ) {
      this.analyzerServiceProvider = analyzerServiceProvider
      this.userManagerServiceProvider = userManagerServiceProvider
      this.importServiceProvider = importServiceProvider
   }
   
   @Override
   public boolean consumes(RemoteEntityImports importType) {
      return importType == RemoteEntityImports.USERS
   }
   
   @Override
   public ImportResult importRemoteEntity(ImportConfig config, AbstractNode targetNode, String requestedRemoteEntity) {
      return doImportRemoteEntity(config, targetNode, false, [:], requestedRemoteEntity)
   }

   @Override
   public Map<String, String> checkImportRemoteEntity(ImportConfig config, AbstractNode targetNode,
         Map<String, String> previousCheckResults, String requestedRemoteEntity) {
      return doImportRemoteEntity(config, targetNode, true, previousCheckResults, requestedRemoteEntity)
   }

   private doImportRemoteEntity(ImportConfig config, AbstractNode targetNode, 
         boolean check, Map<String, String> results, String requestedRemoteEntity) {
      if (!(targetNode instanceof OrganisationalUnit)) {
         handleError(check, "Node is not an organizational unit: '$targetNode'", results, IllegalArgumentException)
         if (check)
            return results
      }
         
      def analyzerService = analyzerServiceProvider.get()
      def treeConfig = new TreeNodeImporterConfig()
      config.addSpecificImporterConfigs treeConfig

      def exportRoot = analyzerService.getRoot(config.exportDataProvider, UserManagerExporter)
      if(!exportRoot) {
         handleError(check, 'Could not find root', results, IllegalStateException)
         if (check)
            return results
      }
      def exportRootType = exportRoot.type
      if (exportRootType != OrganisationalUnit) // in case we only exported one item
         exportRoot = targetNode

      /* one more loop to configure user import */
      analyzerService.getExportedItemsFor(config.exportDataProvider, UserManagerExporter)
         .findAll { // filter out elements that should not have been exported, e.g. root
            def parentProp = it.getPropertyByName('parent')
            if (parentProp)
               return true
            def usernameProp = it.getPropertyByName('username')
            if (!usernameProp) //OUs, Groups
               return true
            if (usernameProp.element.value != requestedRemoteEntity) 
               return false
               
            return true
         }.each {
            def parentProp = it.getPropertyByName('parent')
            def usernameProp = it.getPropertyByName('username')
            
            if(usernameProp && userManagerServiceProvider.get().getUserByName(usernameProp.element.value)) 
               handleError(check, "Username '${usernameProp.element.value}' already exists", results, IllegalArgumentException)
            else 
               importServiceProvider.get().configureParents it, config, exportRoot.id as String, targetNode, UserManagerExporter
      }

      /* complete import */
      if (check)
         return results
      else
         return importServiceProvider.get().importData(config)
   }
   
}
