package net.datenwerke.rs.fileserver.service.fileserver.eximport.hookers;

import com.google.inject.Inject;

import net.datenwerke.eximport.ex.ExportConfig;
import net.datenwerke.rs.eximport.service.eximport.hooks.ExportAllHook;
import net.datenwerke.rs.fileserver.service.fileserver.FileServerService;
import net.datenwerke.treedb.ext.service.eximport.TreeNodeExImportOptions;
import net.datenwerke.treedb.ext.service.eximport.TreeNodeExportItemConfig;
import net.datenwerke.treedb.ext.service.eximport.TreeNodeExporterConfig;
import net.datenwerke.treedb.service.treedb.AbstractNode;

public class ExportAllFilesHooker implements ExportAllHook {

	private final FileServerService fileService;
	
	@Inject
	public ExportAllFilesHooker(FileServerService fileService) {
		this.fileService = fileService;
	}

	@Override
	public void configure(ExportConfig config) {
		TreeNodeExporterConfig specConfig = new TreeNodeExporterConfig();
		specConfig.addExImporterOptions(TreeNodeExImportOptions.INCLUDE_OWNER, TreeNodeExImportOptions.INCLUDE_SECURITY);
		config.addSpecificExporterConfigs(specConfig);
		
		for(AbstractNode<?> node : fileService.getAllNodes())
			config.addItemConfig(new TreeNodeExportItemConfig(node));
	}

}
