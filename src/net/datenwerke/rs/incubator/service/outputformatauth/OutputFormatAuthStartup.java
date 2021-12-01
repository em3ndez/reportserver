package net.datenwerke.rs.incubator.service.outputformatauth;

import net.datenwerke.hookhandler.shared.hookhandler.HookHandlerService;
import net.datenwerke.rs.core.service.reportmanager.hooks.ReportExecutionNotificationHook;
import net.datenwerke.rs.incubator.service.outputformatauth.hookers.OutputFormatAuthWatchDog;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class OutputFormatAuthStartup {

	@Inject
	public OutputFormatAuthStartup(
		HookHandlerService hookHandler,
		
		Provider<OutputFormatAuthWatchDog> outputFormatAuthWatchdog
		){
		
		hookHandler.attachHooker(ReportExecutionNotificationHook.class, outputFormatAuthWatchdog);
		
	}
}
