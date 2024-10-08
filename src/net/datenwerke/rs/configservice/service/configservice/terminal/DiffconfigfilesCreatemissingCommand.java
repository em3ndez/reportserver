package net.datenwerke.rs.configservice.service.configservice.terminal;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.inject.Inject;

import net.datenwerke.gf.service.history.HistoryLink;
import net.datenwerke.gf.service.history.HistoryService;
import net.datenwerke.rs.configservice.service.configservice.ConfigService;
import net.datenwerke.rs.configservice.service.configservice.locale.ConfigMessages;
import net.datenwerke.rs.fileserver.service.fileserver.FileServerService;
import net.datenwerke.rs.fileserver.service.fileserver.entities.AbstractFileServerNode;
import net.datenwerke.rs.fileserver.service.fileserver.entities.FileServerFile;
import net.datenwerke.rs.fileserver.service.fileserver.entities.FileServerFolder;
import net.datenwerke.rs.terminal.service.terminal.TerminalSession;
import net.datenwerke.rs.terminal.service.terminal.exceptions.TerminalException;
import net.datenwerke.rs.terminal.service.terminal.helpers.CommandParser;
import net.datenwerke.rs.terminal.service.terminal.helpmessenger.annotations.CliHelpMessage;
import net.datenwerke.rs.terminal.service.terminal.obj.CommandResult;
import net.datenwerke.rs.terminal.service.terminal.obj.CommandResultHyperlink;
import net.datenwerke.rs.terminal.service.terminal.obj.CommandResultLine;
import net.datenwerke.rs.utils.string.Emoji;
import net.datenwerke.security.service.security.SecurityService;
import net.datenwerke.security.service.security.SecurityTarget;
import net.datenwerke.security.service.security.rights.Read;
import net.datenwerke.security.service.security.rights.Write;

public class DiffconfigfilesCreatemissingCommand extends DiffconfigfilesSubCommand {
   private static final String BASE_COMMAND = "createmissing";
   private static final String REGEX_TO_MATCH_SLASHFILENAME_STRING = "/(?:.(?!/))+$";

   @Inject
   public DiffconfigfilesCreatemissingCommand(
         HistoryService historyService, 
         FileServerService fileServerService,
         ConfigService configService, 
         SecurityService securityService
         ) {
      super(historyService, fileServerService, configService, securityService, BASE_COMMAND);
   }

   @CliHelpMessage(
         messageClass = ConfigMessages.class, 
         name = BASE_COMMAND, 
         description = "commandDiffConfigFiles_sub_createmissing_description"
         )
   @Override
   public CommandResult execute(CommandParser parser, TerminalSession session) throws TerminalException {
      FileServerFolder root = (FileServerFolder) fileServerService.getRoots().get(0);
      FileServerFolder etc = root.getSubfolderByName("etc");
      securityService.assertRights((SecurityTarget) root, Read.class);
      securityService.assertRights((SecurityTarget) etc, Read.class, Write.class);

      List<HistoryLink> missingConfigFileLinks = null;
      List<FileServerFile> newFilesInActualConfig = null;
      try {
         createTmpConfigFolderAndSetFolderNameAndPath();
         configService.extractBasicConfigFilesTo(super.tmpDirName);
         missingConfigFileLinks = findMissingConfigFiles(super.tmpConfigFolder);
         moveMissingConfigFiles(missingConfigFileLinks);
         newFilesInActualConfig = missingConfigFileLinks
               .stream()
               .map(fileLink -> findFileInActualConfig(fileLink))
               .collect(toList());
      } catch (Exception e) {
         throw new TerminalException(Emoji.exceptionEmoji().getEmoji(" ") + "the config files could not be calculated: " + e.getMessage(), e);
      } finally {
         removeTmpConfigFolder();
      }
      return generateCommandResult(newFilesInActualConfig);
   }

   private void moveMissingConfigFiles(List<HistoryLink> missingConfigFileLinks) {
      missingConfigFileLinks.forEach(link -> {
         AbstractFileServerNode fileToMove = fileServerService.getNodeByPath(getFilePathFromHistoryLink(link), false);
         String dstFolderPath = getDstFolderPathFromHistoryLink(link);
         fileServerService.createFolderAtLocation("/fileserver/" + dstFolderPath);
         AbstractFileServerNode dstFolder = fileServerService.getNodeByPath(dstFolderPath,
               false);
         fileServerService.copy(fileToMove, dstFolder, true);
      });
   }

   private String getFilePathFromHistoryLink(HistoryLink link) {
      return link.getObjectCaption().split("Root/")[1];
   }

   private String getDstFolderPathFromHistoryLink(HistoryLink link) {
      String filePath = getFilePathFromHistoryLink(link);
      return filePath.replace(tmpDirName + "/", "").replaceFirst(REGEX_TO_MATCH_SLASHFILENAME_STRING, "");
   }

   private CommandResult generateCommandResult(List<FileServerFile> copiedFiles) {
      if (copiedFiles.isEmpty())
         return new CommandResult(Emoji.CLINKING_BEER_MUGS.getEmoji(" ") + "No missing files detected - no files were copied");
      CommandResult commandResult = new CommandResult();
      List<CommandResultHyperlink> hyperLinkEntries = copiedFiles.stream()
            .map(file -> historyService.buildLinksFor(file))
            .map(listHistoryLinks -> listHistoryLinks.get(0))
            .map(historyLink -> new CommandResultHyperlink(
                  historyLink.getObjectCaption() + " (" + historyLink.getHistoryLinkBuilderId() + ")",
                  historyLink.getLink()))
            .collect(toList());

      commandResult.addEntry(new CommandResultLine(Emoji.BEER_MUG.getEmoji(" ") + 
            "The following files were detected as missing and copied to their expected location:"));
      hyperLinkEntries.forEach(entry -> commandResult.addEntry(entry));
      return commandResult;
   }

   private FileServerFile findFileInActualConfig(HistoryLink link) {
      Pattern fileNamePattern = Pattern.compile(REGEX_TO_MATCH_SLASHFILENAME_STRING);
      Matcher m = fileNamePattern.matcher(link.getObjectCaption());
      String matchedString = (m.find()) ? m.group(0) : "";
      String expectedFileName = matchedString.replace("/", "");
      String dstFolderPath = getDstFolderPathFromHistoryLink(link);

      FileServerFolder expectedFolder = (FileServerFolder) fileServerService.getNodeByPath(dstFolderPath, false);
      List<FileServerFile> files = expectedFolder.getChildrenOfType(FileServerFile.class);
      Optional<FileServerFile> findAny = files
            .stream()
            .filter(file -> file.getName().equals(expectedFileName))
            .findAny();
      if (!findAny.isPresent())
         throw new IllegalArgumentException(Emoji.exceptionEmoji().getEmoji(" ") + "Not found: " + expectedFileName);
      return findAny.get();
   }
}
