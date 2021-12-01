package net.datenwerke.rs.incubator.client.jasperutils;

import net.datenwerke.gf.client.managerhelper.hooks.MainPanelViewToolbarConfiguratorHook;
import net.datenwerke.hookhandler.shared.hookhandler.HookHandlerService;
import net.datenwerke.rs.incubator.client.jasperutils.hookers.JasperReportParameterProposerToolbarConfiguratorHooker;

import com.google.inject.Inject;

public class JasperUIStartup {

	@Inject
	public JasperUIStartup(
		HookHandlerService hookHandler,
		
		JasperReportParameterProposerToolbarConfiguratorHooker parameterProposer
		){
		
		
		hookHandler.attachHooker(MainPanelViewToolbarConfiguratorHook.class, parameterProposer);
	}
}
