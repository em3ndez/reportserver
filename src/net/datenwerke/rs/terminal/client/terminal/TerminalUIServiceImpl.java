package net.datenwerke.rs.terminal.client.terminal;

import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.HTML;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.sencha.gxt.core.client.dom.ScrollSupport.ScrollMode;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;

import net.datenwerke.gf.client.history.HistoryLocation;
import net.datenwerke.gxtdto.client.baseex.widget.DwWindow;
import net.datenwerke.gxtdto.client.dtomanager.Dto2PosoMapper;
import net.datenwerke.hookhandler.shared.hookhandler.HookHandlerService;
import net.datenwerke.rs.terminal.client.terminal.dto.CommandResultDto;
import net.datenwerke.rs.terminal.client.terminal.helper.DisplayHelper;
import net.datenwerke.rs.terminal.client.terminal.hooks.CommandResultProcessorHook;
import net.datenwerke.rs.terminal.client.terminal.ui.TerminalWindow;
import net.datenwerke.treedb.client.treedb.dto.AbstractNodeDto;

/**
 * 
 *
 */
public class TerminalUIServiceImpl implements TerminalUIService {

   private final HookHandlerService hookHandler;
   private final DisplayHelper displayHelper;
   private final Provider<TerminalWindow> terminalWindowProvider;

   private boolean initialized;

   @Inject
   public TerminalUIServiceImpl(
         HookHandlerService hookHandler, DisplayHelper displayHelper,
         Provider<TerminalWindow> terminalWindowProvider
         ) {

      /* store objects */
      this.hookHandler = hookHandler;
      this.displayHelper = displayHelper;
      this.terminalWindowProvider = terminalWindowProvider;
   }

   @Override
   public void initTerminal() {
      if (isInitialized())
         return;

      initialized = true;

      Event.addNativePreviewHandler(event -> {
         if (event.isCanceled() || event.isConsumed())
            return;

         if (event.getTypeInt() == Event.ONKEYUP) {
            boolean ctrlKey = event.getNativeEvent().getCtrlKey();
            boolean altKey = event.getNativeEvent().getAltKey();
            if (ctrlKey && altKey) {
               int keyCode = event.getNativeEvent().getKeyCode();
               if (keyCode == 'T' || keyCode == 't') {
                  if (event.isConsumed())
                     return;
                  event.consume();
                  displayTerminalWindow();
               }
            }
         }
      });
   }

   @Override
   public boolean isInitialized() {
      return initialized;
   }

   @Override
   public void displayTerminalWindow() {
      if (!isInitialized())
         initTerminal();

      terminalWindowProvider.get().show();
   }
   
   @Override
   public void displayTerminalWindow(AbstractNodeDto node, Dto2PosoMapper dto2PosoMapper) {
      final TerminalWindow terminal = terminalWindowProvider.get();
      terminal.initSessionInNodeLocation(node, dto2PosoMapper);
      terminal.show();
   }

   @Override
   public void processExternalResult(CommandResultDto result) {
      if (null == result)
         return;
      
      hookHandler.getHookers(CommandResultProcessorHook.class)
         .forEach(processor -> processor.process(result));
   }

   @Override
   public void displayResult(CommandResultDto result) {
      DwWindow window = new DwWindow();
      window.setSize(640, 480);
      window.setOnEsc(true);

      VerticalLayoutContainer container = new VerticalLayoutContainer();
      container.setScrollMode(ScrollMode.AUTO);
      container.add(new HTML(displayHelper.format(result)));

      window.add(container);
      window.show();
   }

   @Override
   public void displayTerminalMaximizedWindow() {
      final TerminalWindow terminal = terminalWindowProvider.get();
      terminal.show();
      terminal.maximize();
      terminal.getHeader().hide();
   }

   @Override
   public void displayTerminalMaximizedWindowWithCommand(HistoryLocation location) {
      String path = URL.decodePathSegment(location.getParameterValue("terminalpath"));
      final TerminalWindow terminal = terminalWindowProvider.get();
      terminal.initSessionWithPath(path);
      terminal.show();
      terminal.maximize();
      terminal.getHeader().hide();
   }

}
