package net.datenwerke.rs.terminal.service.terminal.locale;

import net.datenwerke.rs.utils.localization.LocalizationServiceImpl;
import net.datenwerke.rs.utils.localization.Messages;

public interface TerminalMessages extends Messages {

   public final static TerminalMessages INSTANCE = LocalizationServiceImpl.getMessages(TerminalMessages.class);

   String commandDesc_description();

   String commandDesc_descFlag();

   String commandDesc_typeArgument();

   String commandDesc_objectsArgument();

   String commandEliza_description();

   String commandHelloWorld_description();

   String commandMeminfo_description();

   String commandHql_description();

   String commandHql_wFlag();

   String commandHql_hqlArgument();

   String helpDescription();

   String usage();

   String helpFlagDescription();

   String locationDoesNotExistException();

   String locationIsNoFolderException();

   String nodeDoesNotExistException();

   String commandCat_description();

   String cannotCatObject();

   String commandEnv_description();
   
   String commandSsltest_description();
   
   String commandSsltest_host();
   
   String commandSsltest_port();
   
   String commandInfo_desc();
   
   String commandInfoDatasource_desc();
   
   String commandInfoDatasource_datasource();
   
   String commandCopydesc();
   
   String commandCopyParameterDefinitions_desc();
   
   String commandCopyParameterDefinitions_from();
   
   String commandCopyParameterDefinitions_to();
   
   String commandCopyParameterDefinitions_replaceExistingParameters();
   
}
