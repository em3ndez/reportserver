package net.datenwerke.rs.samba.client.samba.hookers;

import static net.datenwerke.rs.core.client.datasinkmanager.helper.forms.simpleform.DatasinkSelectionFieldProvider.extractSingleTreeSelectionField;

import java.util.Collection;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.sencha.gxt.widget.core.client.form.FormPanel.LabelAlign;

import net.datenwerke.gf.client.treedb.UITree;
import net.datenwerke.gf.client.treedb.selection.SingleTreeSelectionField;
import net.datenwerke.gf.client.treedb.simpleform.SFFCGenericTreeNode;
import net.datenwerke.gxtdto.client.dtomanager.callback.RsAsyncCallback;
import net.datenwerke.gxtdto.client.forms.simpleform.SimpleForm;
import net.datenwerke.gxtdto.client.forms.simpleform.actions.ShowHideFieldAction;
import net.datenwerke.gxtdto.client.forms.simpleform.conditions.FieldEquals;
import net.datenwerke.gxtdto.client.forms.simpleform.providers.configs.SFFCAllowBlank;
import net.datenwerke.gxtdto.client.forms.simpleform.providers.configs.SFFCBoolean;
import net.datenwerke.gxtdto.client.forms.simpleform.providers.configs.SFFCDatasinkDao;
import net.datenwerke.gxtdto.client.locale.BaseMessages;
import net.datenwerke.rs.core.client.datasinkmanager.DatasinkTreeManagerDao;
import net.datenwerke.rs.core.client.datasinkmanager.DatasinkUIModule;
import net.datenwerke.rs.core.client.datasinkmanager.HasDefaultDatasink;
import net.datenwerke.rs.core.client.datasinkmanager.helper.forms.DatasinkSelectionField;
import net.datenwerke.rs.core.client.reportexecutor.ui.ReportViewConfiguration;
import net.datenwerke.rs.core.client.reportmanager.dto.reports.ReportDto;
import net.datenwerke.rs.samba.client.samba.SambaDao;
import net.datenwerke.rs.samba.client.samba.SambaUiModule;
import net.datenwerke.rs.samba.client.samba.dto.SambaDatasinkDto;
import net.datenwerke.rs.samba.client.samba.dto.ScheduleAsSambaFileInformation;
import net.datenwerke.rs.samba.client.samba.provider.annotations.DatasinkTreeSamba;
import net.datenwerke.rs.scheduleasfile.client.scheduleasfile.locale.ScheduleAsFileMessages;
import net.datenwerke.rs.scheduler.client.scheduler.dto.ReportScheduleDefinition;
import net.datenwerke.rs.scheduler.client.scheduler.hooks.ScheduleExportSnippetProviderHook;
import net.datenwerke.rs.scheduler.client.scheduler.locale.SchedulerMessages;
import net.datenwerke.rs.scheduler.client.scheduler.schedulereport.pages.JobMetadataConfigurationForm;
import net.datenwerke.rs.theme.client.icon.BaseIcon;

public class SambaExportSnippetProvider implements ScheduleExportSnippetProviderHook {

   private String isExportAsSambaKey;
   private String folderKey;
   private String nameKey;
   private String sambaKey;
   private String compressedKey;

   private final Provider<UITree> treeProvider;
   private final Provider<SambaDao> datasinkDaoProvider;
   private final DatasinkTreeManagerDao datasinkTreeManager;

   @Inject
   public SambaExportSnippetProvider(@DatasinkTreeSamba Provider<UITree> treeProvider,
         DatasinkTreeManagerDao datasinkTreeManager, Provider<SambaDao> datasinkDaoProvider) {
      this.treeProvider = treeProvider;
      this.datasinkTreeManager = datasinkTreeManager;
      this.datasinkDaoProvider = datasinkDaoProvider;
   }

   @Override
   public boolean appliesFor(ReportDto report, Collection<ReportViewConfiguration> configs) {
      return true;
   }

   @Override
   public void configureSimpleForm(final SimpleForm xform, ReportDto report,
         Collection<ReportViewConfiguration> configs) {

      xform.setLabelAlign(LabelAlign.LEFT);
      isExportAsSambaKey = xform.addField(Boolean.class, "", new SFFCBoolean() {
         @Override
         public String getBoxLabel() {
            return SambaUiModule.NAME;
         }
      });
      xform.setLabelAlign(LabelAlign.TOP);

      xform.setFieldWidth(260);
      xform.beginFloatRow();

      sambaKey = xform.addField(DatasinkSelectionField.class, SambaUiModule.NAME, new SFFCGenericTreeNode() {
         @Override
         public UITree getTreeForPopup() {
            return treeProvider.get();
         }
      }, new SFFCAllowBlank() {
         @Override
         public boolean allowBlank() {
            return false;
         }
      }, new SFFCDatasinkDao() {
         @Override
         public Provider<? extends HasDefaultDatasink> getDatasinkDaoProvider() {
            return datasinkDaoProvider;
         }

         @Override
         public BaseIcon getIcon() {
            return SambaUiModule.ICON;
         }
      });

      folderKey = xform.addField(String.class, ScheduleAsFileMessages.INSTANCE.folder(), new SFFCAllowBlank() {
         @Override
         public boolean allowBlank() {
            return false;
         }
      });

      xform.endRow();
      xform.setFieldWidth(1);

      nameKey = xform.addField(String.class, BaseMessages.INSTANCE.propertyName(), new SFFCAllowBlank() {
         @Override
         public boolean allowBlank() {
            return false;
         }
      });

      xform.setLabelAlign(LabelAlign.LEFT);
      compressedKey = xform.addField(Boolean.class, "", new SFFCBoolean() {
         @Override
         public String getBoxLabel() {
            return SchedulerMessages.INSTANCE.reportCompress();
         }
      });

      xform.addCondition(isExportAsSambaKey, new FieldEquals(true), new ShowHideFieldAction(folderKey));
      xform.addCondition(isExportAsSambaKey, new FieldEquals(true), new ShowHideFieldAction(nameKey));
      xform.addCondition(isExportAsSambaKey, new FieldEquals(true), new ShowHideFieldAction(sambaKey));
      xform.addCondition(isExportAsSambaKey, new FieldEquals(true), new ShowHideFieldAction(compressedKey));

   }

   @Override
   public boolean isValid(SimpleForm simpleForm) {
      simpleForm.clearInvalid();
      return simpleForm.isValid();
   }

   @Override
   public void configureConfig(ReportScheduleDefinition configDto, SimpleForm form) {
      if (!isActive(form))
         return;

      ScheduleAsSambaFileInformation info = new ScheduleAsSambaFileInformation();
      info.setName((String) form.getValue(nameKey));
      info.setFolder((String) form.getValue(folderKey));
      info.setCompressed((Boolean) form.getValue(compressedKey));
      info.setSambaDatasinkDto((SambaDatasinkDto) form.getValue(sambaKey));

      configDto.addAdditionalInfo(info);
   }

   @Override
   public boolean isActive(SimpleForm simpleForm) {
      return (Boolean) simpleForm.getValue(isExportAsSambaKey);
   }

   @Override
   public void loadFields(final SimpleForm form, ReportScheduleDefinition definition, ReportDto report) {
      form.loadFields();

      final SingleTreeSelectionField sambaField = extractSingleTreeSelectionField(form.getField(sambaKey));

      if (null != definition) {
         form.setValue(nameKey, DatasinkUIModule.DEFAULT_FILE_NAME);
         ScheduleAsSambaFileInformation info = definition.getAdditionalInfo(ScheduleAsSambaFileInformation.class);
         if (null != info) {
            form.setValue(isExportAsSambaKey, true);
            form.setValue(nameKey, info.getName());
            form.setValue(folderKey, info.getFolder());
            form.setValue(compressedKey, info.isCompressed());
            sambaField.setValue(info.getSambaDatasinkDto());
         }
      }

      sambaField.addValueChangeHandler(event -> {
         if (null == event.getValue())
            return;

         datasinkTreeManager.loadFullViewNode((SambaDatasinkDto) event.getValue(),
               new RsAsyncCallback<SambaDatasinkDto>() {
                  @Override
                  public void onSuccess(SambaDatasinkDto result) {
                     form.setValue(folderKey, result.getFolder());
                  }

                  @Override
                  public void onFailure(Throwable caught) {
                     super.onFailure(caught);
                  }
               });

      });
   }

   @Override
   public void onWizardPageChange(int pageNr, Widget page, SimpleForm form, ReportScheduleDefinition definition,
         ReportDto report) {
      if (!(page instanceof JobMetadataConfigurationForm))
         return;

      form.setValue(nameKey, DatasinkUIModule.DEFAULT_FILE_NAME);
      if (null != definition) {
         ScheduleAsSambaFileInformation info = definition.getAdditionalInfo(ScheduleAsSambaFileInformation.class);
         if (null != info)
            form.setValue(nameKey, info.getName());
      }
   }

}