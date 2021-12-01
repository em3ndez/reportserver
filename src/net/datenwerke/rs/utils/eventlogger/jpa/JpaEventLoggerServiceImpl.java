package net.datenwerke.rs.utils.eventlogger.jpa;

import net.datenwerke.rs.utils.daemon.DwDaemonServiceImpl;
import net.datenwerke.rs.utils.daemon.DwDaemonWatchdog;
import net.datenwerke.rs.utils.eventlogger.EventLoggerService;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class JpaEventLoggerServiceImpl extends DwDaemonServiceImpl<JpaEventLoggerDaemon> implements EventLoggerService {

	@Inject
	public JpaEventLoggerServiceImpl(
		Provider<JpaEventLoggerDaemon> daemonProvider,
		Provider<DwDaemonWatchdog> watchdogProvider
		){
		super(daemonProvider, watchdogProvider);
	}
	
	
	
}
