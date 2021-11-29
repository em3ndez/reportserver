package net.datenwerke.rs.samba.service.samba;

import java.util.Optional;

import com.google.inject.ImplementedBy;

import net.datenwerke.rs.core.service.datasinkmanager.BasicDatasinkService;
import net.datenwerke.rs.core.service.datasinkmanager.exceptions.DatasinkExportException;
import net.datenwerke.rs.samba.service.samba.definitions.SambaDatasink;

@ImplementedBy(DummySambaServiceImpl.class)
public interface SambaService extends BasicDatasinkService {

   /**
    * Sends a report to a Samba server defined in a given {@link SambaDatasink}
    * <tt>datasink</tt>. The folder defined in the {@link SambaDatasink} is
    * overridden by the <tt>folder</tt> parameter.
    * 
    * @param report        the report to send. May be a String or a byte array
    * @param sambaDatasink defines the datasink to use
    * @param filename      filename to use for the report
    * @param folder        extension path of the base path defined in the datasink.
    *                      Overrides the folder defined in the
    *                      {@link SambaDatasink}
    * @throws DatasinkExportException if an error occurs during datasink export
    */
   void exportIntoDatasink(Object report, SambaDatasink sambaDatasink, String filename, String folder)
         throws DatasinkExportException;

   /**
    * Issues a Samba test request by creating a simple text file and sending it to
    * the specified directory in the Samba server of the datasink.
    * 
    * @param sambaDatasink the {@link SambaDatasink} to test
    * @throws DatasinkExportException if an error occurs during datasink export
    */
   void testDatasink(SambaDatasink sambaDatasink) throws DatasinkExportException;

   Optional<SambaDatasink> getDefaultDatasink();
}
