package net.datenwerke.rs.base.client.datasources.hooks;

import net.datenwerke.hookhandler.shared.hookhandler.interfaces.Hook;
import net.datenwerke.rs.base.client.datasources.dto.DatasourceConnectorDto;
import net.datenwerke.rs.base.client.datasources.dto.FormatBasedDatasourceDefinitionDto;

import com.google.gwt.user.client.ui.Widget;

public interface DatasourceConnectorConfiguratorHook extends Hook {

	Widget configureForm(FormatBasedDatasourceDefinitionDto datasource);

	DatasourceConnectorDto instantiateConnector();
	String getConnectorName();
	
	boolean consumes(DatasourceConnectorDto connector);

}
