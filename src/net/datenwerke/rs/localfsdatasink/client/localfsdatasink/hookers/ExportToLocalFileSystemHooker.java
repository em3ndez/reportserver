package net.datenwerke.rs.localfsdatasink.client.localfsdatasink.hookers;

import static net.datenwerke.rs.core.client.datasinkmanager.helper.forms.simpleform.DatasinkSimpleFormProvider.extractSingleTreeSelectionField;

import java.util.Collection;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.sencha.gxt.widget.core.client.container.MarginData;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.info.DefaultInfoConfig;
import com.sencha.gxt.widget.core.client.info.Info;
import com.sencha.gxt.widget.core.client.info.InfoConfig;
import com.sencha.gxt.widget.core.client.menu.Menu;
import com.sencha.gxt.widget.core.client.menu.MenuItem;

import net.datenwerke.gf.client.treedb.UITree;
import net.datenwerke.gf.client.treedb.selection.SingleTreeSelectionField;
import net.datenwerke.gf.client.treedb.simpleform.SFFCGenericTreeNode;
import net.datenwerke.gxtdto.client.baseex.widget.DwContentPanel;
import net.datenwerke.gxtdto.client.baseex.widget.DwWindow;
import net.datenwerke.gxtdto.client.baseex.widget.btn.DwTextButton;
import net.datenwerke.gxtdto.client.baseex.widget.mb.DwAlertMessageBox;
import net.datenwerke.gxtdto.client.baseex.widget.menu.DwMenuItem;
import net.datenwerke.gxtdto.client.dtomanager.callback.RsAsyncCallback;
import net.datenwerke.gxtdto.client.forms.simpleform.SimpleForm;
import net.datenwerke.gxtdto.client.forms.simpleform.providers.configs.SFFCAllowBlank;
import net.datenwerke.gxtdto.client.forms.simpleform.providers.configs.SFFCBoolean;
import net.datenwerke.gxtdto.client.forms.simpleform.providers.configs.SFFCDatasinkDao;
import net.datenwerke.gxtdto.client.locale.BaseMessages;
import net.datenwerke.gxtdto.client.servercommunication.callback.NotamCallback;
import net.datenwerke.hookhandler.shared.hookhandler.HookHandlerService;
import net.datenwerke.rs.core.client.datasinkmanager.DatasinkTreeManagerDao;
import net.datenwerke.rs.core.client.datasinkmanager.HasDefaultDatasink;
import net.datenwerke.rs.core.client.datasinkmanager.helper.forms.DatasinkSelectionField;
import net.datenwerke.rs.core.client.helper.simpleform.ExportTypeSelection;
import net.datenwerke.rs.core.client.helper.simpleform.config.SFFCExportTypeSelector;
import net.datenwerke.rs.core.client.reportexecutor.hooks.PrepareReportModelForStorageOrExecutionHook;
import net.datenwerke.rs.core.client.reportexecutor.ui.ReportExecutorInformation;
import net.datenwerke.rs.core.client.reportexecutor.ui.ReportExecutorMainPanel;
import net.datenwerke.rs.core.client.reportexecutor.ui.ReportViewConfiguration;
import net.datenwerke.rs.core.client.reportexporter.hooks.ExportExternalEntryProviderHook;
import net.datenwerke.rs.core.client.reportexporter.locale.ReportExporterMessages;
import net.datenwerke.rs.core.client.reportmanager.dto.reports.ReportDto;
import net.datenwerke.rs.enterprise.client.EnterpriseUiService;
import net.datenwerke.rs.eximport.client.eximport.locale.ExImportMessages;
import net.datenwerke.rs.localfsdatasink.client.localfsdatasink.LocalFileSystemDao;
import net.datenwerke.rs.localfsdatasink.client.localfsdatasink.LocalFileSystemUiModule;
import net.datenwerke.rs.localfsdatasink.client.localfsdatasink.dto.LocalFileSystemDatasinkDto;
import net.datenwerke.rs.localfsdatasink.client.localfsdatasink.provider.annotations.DatasinkTreeLocalFileSystem;
import net.datenwerke.rs.scheduleasfile.client.scheduleasfile.StorageType;
import net.datenwerke.rs.scheduleasfile.client.scheduleasfile.locale.ScheduleAsFileMessages;
import net.datenwerke.rs.scheduler.client.scheduler.locale.SchedulerMessages;
import net.datenwerke.rs.theme.client.icon.BaseIcon;

public class ExportToLocalFileSystemHooker implements ExportExternalEntryProviderHook {

	private final HookHandlerService hookHandler;

	private final Provider<UITree> treeProvider;
	private final DatasinkTreeManagerDao datasinkTreeManager;
	private final Provider<LocalFileSystemDao> datasinkDaoProvider;
	
	private final Provider<EnterpriseUiService> enterpriseServiceProvider;
	
	@Inject
	public ExportToLocalFileSystemHooker(
	      HookHandlerService hookHandler,
			DatasinkTreeManagerDao datasinkTreeManager,
			@DatasinkTreeLocalFileSystem Provider<UITree> treeProvider,
			Provider<LocalFileSystemDao> datasinkDaoProvider,
			Provider<EnterpriseUiService> enterpriseServiceProvider
			) {
		this.hookHandler = hookHandler;
		this.treeProvider = treeProvider;
		this.datasinkTreeManager = datasinkTreeManager;
		this.datasinkDaoProvider = datasinkDaoProvider;
		this.enterpriseServiceProvider = enterpriseServiceProvider;
	}

    @Override
    public void getMenuEntry(final Menu menu, final ReportDto report, final ReportExecutorInformation info,
          final ReportExecutorMainPanel mainPanel) {
       if (enterpriseServiceProvider.get().isEnterprise()) {
          datasinkDaoProvider.get().getStorageEnabledConfigs(new AsyncCallback<Map<StorageType, Boolean>>() {
   
             @Override
             public void onSuccess(Map<StorageType, Boolean> result) {
                // item is only added if enabled in configuration
                if (result.get(StorageType.LOCALFILESYSTEM)) {
                   MenuItem item = new DwMenuItem(LocalFileSystemUiModule.NAME, LocalFileSystemUiModule.ICON);
                   menu.add(item);
                   item.addSelectionHandler(event -> displayExportDialog(report, info, mainPanel.getViewConfigs()));
                }
             }
   
             @Override
             public void onFailure(Throwable caught) {
             }
          });
       } else {
          // we add item but disable it
          MenuItem item = new DwMenuItem(LocalFileSystemUiModule.NAME, LocalFileSystemUiModule.ICON);
          menu.add(item);
          item.disable();
       }
    }

	protected void displayExportDialog(final ReportDto report, final ReportExecutorInformation info,
			Collection<ReportViewConfiguration> configs) {
		final DwWindow window = new DwWindow();
		window.setHeaderIcon(LocalFileSystemUiModule.ICON);
		window.setHeading(LocalFileSystemUiModule.NAME);
		window.setWidth(500);
		window.setHeight(360);
		window.setCenterOnShow(true);

		DwContentPanel wrapper = new DwContentPanel();
		wrapper.setHeading(BaseMessages.INSTANCE.configuration());
		wrapper.setLightDarkStyle();
		wrapper.setBodyBorder(false);
		wrapper.setBorders(false);

		VerticalLayoutContainer formWrapper = new VerticalLayoutContainer();

		/* configure form */
		final SimpleForm form = SimpleForm.getInlineInstance();

		formWrapper.add(form);

		form.setFieldWidth(215);
		form.beginFloatRow();

        String localFileSystemKey = form.addField(DatasinkSelectionField.class,
              LocalFileSystemUiModule.NAME, new SFFCGenericTreeNode() {
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
                    return LocalFileSystemUiModule.ICON;
                 }
              });

		final String folderKey = form.addField(String.class, ScheduleAsFileMessages.INSTANCE.folder(),
				new SFFCAllowBlank() {
					@Override
					public boolean allowBlank() {
						return false;
					}
				});

		form.endRow();
		form.setFieldWidth(1);

		final String formatKey = form.addField(ExportTypeSelection.class,
				ScheduleAsFileMessages.INSTANCE.exportTypeLabel(), new SFFCExportTypeSelector() {
					@Override
					public ReportDto getReport() {
						return report;
					}

					@Override
					public String getExecuteReportToken() {
						return info.getExecuteReportToken();
					}
				});

		final String nameKey = form.addField(String.class, ScheduleAsFileMessages.INSTANCE.nameLabel(),
				new SFFCAllowBlank() {
					@Override
					public boolean allowBlank() {
						return false;
					}
				});
		
	    final String compressedKey = form.addField(Boolean.class, "", new SFFCBoolean() {
	       @Override
	       public String getBoxLabel() {
	          return SchedulerMessages.INSTANCE.reportCompress();
	       }
	    });

		wrapper.setWidget(formWrapper);
		window.add(wrapper, new MarginData(10));

		/* set properties */
		form.setValue(nameKey, report.getName());

		/* load fields */
		form.loadFields();

		final SingleTreeSelectionField localFileSystemField = extractSingleTreeSelectionField(
				form.getField(localFileSystemKey));

		localFileSystemField.addValueChangeHandler(event -> {
			if (null == event.getValue())
				return;

			datasinkTreeManager.loadFullViewNode((LocalFileSystemDatasinkDto) event.getValue(),
					new RsAsyncCallback<LocalFileSystemDatasinkDto>() {
						@Override
						public void onSuccess(LocalFileSystemDatasinkDto result) {
							form.setValue(folderKey, result.getFolder());
						}

						@Override
						public void onFailure(Throwable caught) {
							super.onFailure(caught);
						}
					});

		});

		window.addCancelButton();

		DwTextButton submitBtn = new DwTextButton(BaseMessages.INSTANCE.submit());
		submitBtn.addSelectHandler(event -> {
			form.clearInvalid();

			if (!form.isValid())
				return;

			String name = ((String) form.getValue(nameKey)).trim();
			String folder = ((String) form.getValue(folderKey)).trim();
	        boolean compressed = (boolean) form.getValue(compressedKey);
			ExportTypeSelection type = (ExportTypeSelection) form.getValue(formatKey);

			if (!type.isConfigured()) {
				new DwAlertMessageBox(BaseMessages.INSTANCE.error(),
						ReportExporterMessages.INSTANCE.exportTypeNotConfigured()).show();
				return;
			}

			hookHandler.getHookers(PrepareReportModelForStorageOrExecutionHook.class).stream()
					.filter(hooker -> hooker.consumes(report))
					.forEach(hooker -> hooker.prepareForExecutionOrStorage(report, info.getExecuteReportToken()));

			InfoConfig infoConfig = new DefaultInfoConfig(ExImportMessages.INSTANCE.quickExportProgressTitle(),
					ExImportMessages.INSTANCE.exportWait());
			infoConfig.setWidth(350);
			infoConfig.setDisplay(3500);
			Info.display(infoConfig);

			datasinkDaoProvider.get().exportIntoLocalFileSystem(report, info.getExecuteReportToken(),
					(LocalFileSystemDatasinkDto) form.getValue(localFileSystemKey), type.getOutputFormat(),
					type.getExportConfiguration(), name, folder, compressed,
					new NotamCallback<Void>(ScheduleAsFileMessages.INSTANCE.dataSent()));
			window.hide();
		});
		window.addButton(submitBtn);

		window.show();
	}

}