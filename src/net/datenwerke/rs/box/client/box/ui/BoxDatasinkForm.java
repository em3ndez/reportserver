package net.datenwerke.rs.box.client.box.ui;

import com.sencha.gxt.widget.core.client.menu.Menu;
import com.sencha.gxt.widget.core.client.menu.MenuItem;

import net.datenwerke.gxtdto.client.baseex.widget.menu.DwMenu;
import net.datenwerke.gxtdto.client.baseex.widget.menu.DwMenuItem;
import net.datenwerke.gxtdto.client.forms.simpleform.SimpleForm;
import net.datenwerke.gxtdto.client.forms.simpleform.providers.configs.SFFCPasswordField;
import net.datenwerke.gxtdto.client.locale.BaseMessages;
import net.datenwerke.rs.box.client.box.dto.BoxDatasinkDto;
import net.datenwerke.rs.box.client.box.dto.pa.BoxDatasinkDtoPA;
import net.datenwerke.rs.core.client.datasinkmanager.locale.DatasinksMessages;
import net.datenwerke.rs.core.client.datasinkmanager.ui.forms.DatasinkSimpleForm;


public class BoxDatasinkForm extends DatasinkSimpleForm {

   @Override
   protected void configureSimpleFormCustomFields(SimpleForm form) {
      form.setFieldWidth(750);
      /* folder */
      form.addField(String.class, BoxDatasinkDtoPA.INSTANCE.folder(), BaseMessages.INSTANCE.folder());

      /* app key */
      form.addField(String.class, BoxDatasinkDtoPA.INSTANCE.appKey(), DatasinksMessages.INSTANCE.appKey());

      /* secret key */
      String secretKey = form.addField(String.class, BoxDatasinkDtoPA.INSTANCE.secretKey(),
            DatasinksMessages.INSTANCE.secretKey(), new SFFCPasswordField() {
               @Override
               public Boolean isPasswordSet() {
                  return ((BoxDatasinkDto) getSelectedNode()).isHasSecretKey();
               }
            }); // $NON-NLS-1$
      Menu clearPwMenu = new DwMenu();
      MenuItem clearPwItem = new DwMenuItem(BaseMessages.INSTANCE.clearPassword());
      clearPwMenu.add(clearPwItem);
      clearPwItem.addSelectionHandler(event -> ((BoxDatasinkDto) getSelectedNode()).setSecretKey(null));
      form.addFieldMenu(secretKey, clearPwMenu);     
   }

}
