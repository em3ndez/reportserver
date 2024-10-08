package net.datenwerke.rs.core.server.datasinks;

import java.util.List;

import javax.persistence.NoResultException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.persist.Transactional;

import groovy.lang.Closure;
import net.datenwerke.gxtdto.client.dtomanager.Dto;
import net.datenwerke.gxtdto.client.servercommunication.exceptions.ExpectedException;
import net.datenwerke.gxtdto.client.servercommunication.exceptions.ServerCallFailedException;
import net.datenwerke.gxtdto.server.dtomanager.DtoService;
import net.datenwerke.rs.core.client.datasinkmanager.dto.DatasinkDefinitionDto;
import net.datenwerke.rs.core.client.datasinkmanager.rpc.DatasinkTreeLoader;
import net.datenwerke.rs.core.client.datasinkmanager.rpc.DatasinkTreeManager;
import net.datenwerke.rs.core.service.datasinkmanager.DatasinkTreeService;
import net.datenwerke.rs.core.service.datasinkmanager.entities.AbstractDatasinkManagerNode;
import net.datenwerke.rs.core.service.datasinkmanager.entities.DatasinkDefinition;
import net.datenwerke.rs.keyutils.service.keyutils.KeyNameGeneratorServiceImpl;
import net.datenwerke.rs.utils.entitycloner.EntityClonerService;
import net.datenwerke.security.server.TreeDBManagerTreeHandler;
import net.datenwerke.security.service.security.SecurityService;
import net.datenwerke.security.service.security.annotation.ArgumentVerification;
import net.datenwerke.security.service.security.annotation.RightsVerification;
import net.datenwerke.security.service.security.annotation.SecurityChecked;
import net.datenwerke.security.service.security.rights.Write;
import net.datenwerke.treedb.client.treedb.dto.AbstractNodeDto;

/**
 * 
 *
 */
@Singleton
public class DatasinkManagerTreeHandlerRpcServiceImpl extends TreeDBManagerTreeHandler<AbstractDatasinkManagerNode>
      implements DatasinkTreeLoader, DatasinkTreeManager {

   /**
    * 
    */
   private static final long serialVersionUID = -455777535667237770L;

   private final DatasinkTreeService datasinkService;

   @Inject
   public DatasinkManagerTreeHandlerRpcServiceImpl(
         DatasinkTreeService datasinkService, 
         DtoService dtoGenerator,
         SecurityService securityService, 
         EntityClonerService entityClonerService,
         KeyNameGeneratorServiceImpl keyGeneratorService
         ) {

      super(datasinkService, 
            dtoGenerator, 
            securityService, 
            entityClonerService,
            keyGeneratorService);

      /* store objects */
      this.datasinkService = datasinkService;
   }

   @Override
   protected boolean allowDuplicateNode(AbstractDatasinkManagerNode node) {
      return node instanceof DatasinkDefinition;
   }

   @Override
   protected void nodeCloned(AbstractDatasinkManagerNode clonedNode, AbstractDatasinkManagerNode realNode) {
      if (!(clonedNode instanceof DatasinkDefinition))
         throw new IllegalArgumentException();
      DatasinkDefinition clone = (DatasinkDefinition) clonedNode;
      DatasinkDefinition real = (DatasinkDefinition) realNode;

      Closure getAllNodes = new Closure(null) {
         public List<AbstractDatasinkManagerNode> doCall() {
            return realNode.getParent().getChildren();
         }
      };
      clone.setName(clone.getName() == null 
            ? keyGeneratorService.getNextCopyName("", getAllNodes)
            : keyGeneratorService.getNextCopyName(clone.getName(), getAllNodes));

      clone.setKey(keyGeneratorService.getNextCopyKey(real.getKey(), datasinkService));
   }
   
   @Override
   protected void doSetInitialProperties(AbstractDatasinkManagerNode inserted) {
      if (inserted instanceof DatasinkDefinition) {
         ((DatasinkDefinition)inserted).setKey(keyGeneratorService.generateDefaultKey(datasinkService));
      }
   }
   
   @SecurityChecked(
         argumentVerification = {
               @ArgumentVerification(
                     name = "node", 
                     isDto = true, 
                     verify = @RightsVerification(
                           rights = Write.class
                     )
               ) 
         }
   )
   @Override
   @Transactional(rollbackOn = { Exception.class })
   public AbstractNodeDto updateNode(@Named("node") AbstractNodeDto node, Dto state) throws ServerCallFailedException {
      /* check if there already is a datasink with the same key */
      if (node instanceof DatasinkDefinitionDto) {
         String key = ((DatasinkDefinitionDto) node).getKey();
         if (null != key && !"".equals(key.trim())) {
            try {
               long id = datasinkService.getDatasinkIdFromKey(((DatasinkDefinitionDto) node).getKey());

               if (id != node.getId())
                  throw new ExpectedException("There already is a datasink with the same key: " + id);

               /*
                * if the datasink id is the same as the id of the datasink to be changed do nothing
                * because this is ok
                */
            } catch (NoResultException e) {
               /* do nothing because this is good */
            }
         }
      }
      return super.updateNode(node, state);
   }
}
