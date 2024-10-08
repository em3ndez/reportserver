package net.datenwerke.rs.amazons3.service.amazons3.definitions;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.apache.commons.codec.binary.Hex;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import com.google.inject.Inject;
import com.google.inject.Provider;

import net.datenwerke.dtoservices.dtogenerator.annotations.AdditionalField;
import net.datenwerke.dtoservices.dtogenerator.annotations.ExposeToClient;
import net.datenwerke.dtoservices.dtogenerator.annotations.GenerateDto;
import net.datenwerke.eximport.ex.annotations.ExportableField;
import net.datenwerke.gf.base.service.annotations.Field;
import net.datenwerke.gf.base.service.annotations.Indexed;
import net.datenwerke.rs.amazons3.service.amazons3.AmazonS3Service;
import net.datenwerke.rs.amazons3.service.amazons3.definitions.dtogen.AmazonS3Datasink2DtoPostProcessor;
import net.datenwerke.rs.amazons3.service.amazons3.locale.AmazonS3DatasinkMessages;
import net.datenwerke.rs.core.service.datasinkmanager.BasicDatasinkService;
import net.datenwerke.rs.core.service.datasinkmanager.FolderedDatasink;
import net.datenwerke.rs.core.service.datasinkmanager.configs.DatasinkConfiguration;
import net.datenwerke.rs.core.service.datasinkmanager.configs.DatasinkFilenameFolderConfig;
import net.datenwerke.rs.core.service.datasinkmanager.entities.DatasinkDefinition;
import net.datenwerke.rs.utils.entitymerge.service.annotations.EntityMergeField;
import net.datenwerke.rs.utils.instancedescription.annotations.InstanceDescription;
import net.datenwerke.rs.utils.misc.DateUtils;
import net.datenwerke.security.service.crypto.pbe.PbeService;
import net.datenwerke.security.service.crypto.pbe.encrypt.EncryptionService;

/**
 * Used to define AmazonS3 datasinks that can be used in ReportServer to send
 * reports to a given Amazon S3 account.
 */
@Entity
@Table(name = "AMAZONS3_DATASINK")
@Audited
@GenerateDto(
      dtoPackage = "net.datenwerke.rs.amazons3.client.amazons3.dto", 
      poso2DtoPostProcessors = AmazonS3Datasink2DtoPostProcessor.class, 
      additionalFields = {
            @AdditionalField(
                  name = "hasSecretKey", 
                  type = Boolean.class
                  ) 
            }, 
            icon = "amazon"
            )
@InstanceDescription(
      msgLocation = AmazonS3DatasinkMessages.class, 
      objNameKey = "amazonS3DatasinkTypeName", 
      icon = "amazon"
      )
@Indexed
public class AmazonS3Datasink extends DatasinkDefinition implements FolderedDatasink {

   /**
    * 
    */
   private static final long serialVersionUID = 1080828242099292320L;

   @Inject
   protected static Provider<PbeService> pbeServiceProvider;
   
   @Inject
   protected static Provider<AmazonS3Service> basicDatasinkService;

   @ExposeToClient
   @Field
   @Type(type = "net.datenwerke.rs.utils.hibernate.RsClobType")
   @EntityMergeField
   private String appKey;

   @ExposeToClient(exposeValueToClient = false, mergeDtoValueBack = true)
   @Type(type = "net.datenwerke.rs.utils.hibernate.RsClobType")
   @EntityMergeField
   @ExportableField(exportField = false)
   private String secretKey;

   @ExposeToClient
   @Field
   @Column(length = 1024)
   @EntityMergeField
   private String folder = "/";

   @ExposeToClient
   @Field
   @Type(type = "net.datenwerke.rs.utils.hibernate.RsClobType")
   @EntityMergeField
   private String bucketName;

   @ExposeToClient
   @Field
   @Column(length = 1024)
   @EntityMergeField
   private String regionName;

   @ExposeToClient
   @Field
   @EntityMergeField
   private String storageClass;

   public String getStorageClass() {
      return storageClass;
   }

   public void setStorageClass(String storageClass) {
      this.storageClass = storageClass;
   }

   public String getBucketName() {
      return bucketName;
   }

   public void setBucketName(String bucketName) {
      this.bucketName = bucketName;
   }

   public String getRegionName() {
      return regionName;
   }

   public void setRegionName(String regionName) {
      this.regionName = regionName;
   }

   @Override
   public String getFolder() {
      return folder;
   }

   public void setFolder(String folder) {
      this.folder = folder;
   }

   public String getAppKey() {
      return appKey;
   }

   public void setAppKey(String appKey) {
      this.appKey = appKey;
   }

   /**
    * Gets the decrypted secret key
    * 
    * @return the decrypted secret key
    */
   public String getSecretKey() {
      if (null == secretKey)
         return null;
      if ("".equals(secretKey))
         return "";

      EncryptionService encryptionService = pbeServiceProvider.get().getEncryptionService();
      return new String(encryptionService.decryptFromHex(secretKey));
   }

   /**
    * Encrypts and sets the given secret
    * 
    * @param secretKey the secret key to encrypt and set
    */

   public void setSecretKey(String secretKey) {
      if (null == secretKey) {
         this.secretKey = null;
         return;
      }

      EncryptionService encryptionService = pbeServiceProvider.get().getEncryptionService();
      byte[] encrypted = encryptionService.encrypt(secretKey);

      this.secretKey = new String(Hex.encodeHex(encrypted));
   }

   @Override
   public BasicDatasinkService getDatasinkService() {
      return basicDatasinkService.get();
   }

   @Override
   public DatasinkConfiguration getDefaultConfiguration(String fileEnding) {
      return new DatasinkFilenameFolderConfig() {

         @Override
         public String getFilename() {
            return DEFAULT_EXPORT_FILENAME + DateUtils.formatCurrentDate() + fileEnding;
         }

         @Override
         public String getFolder() {
            return folder;
         }

      };
   }

}
