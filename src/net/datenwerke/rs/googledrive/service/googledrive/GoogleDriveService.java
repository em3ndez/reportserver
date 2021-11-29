package net.datenwerke.rs.googledrive.service.googledrive;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.json.JSONException;

import com.google.inject.ImplementedBy;

import net.datenwerke.rs.core.service.datasinkmanager.BasicDatasinkService;
import net.datenwerke.rs.core.service.datasinkmanager.exceptions.DatasinkExportException;
import net.datenwerke.rs.googledrive.service.googledrive.definitions.GoogleDriveDatasink;

@ImplementedBy(DummyGoogleDriveServiceImpl.class)
public interface GoogleDriveService extends BasicDatasinkService {
   /**
    * Sends a report to GoogleDrive, defined in a given {@link GoogleDriveDatasink}
    * datasink. The folder defined in the {@link GoogleDriveDatasink} is overridden
    * by the <tt>folder</tt> parameter.
    * 
    * @param report              the report to send. May be a String or a byte
    *                            array
    * @param googleDriveDatasink defines the GoogleDrive datasink to use
    * @param filename            filename to use for the report
    * @param folder              where to save the report in the GoogleDrive
    *                            account. Overrides the folder defined in the
    *                            {@link GoogleDriveDatasink}
    * @throws DatasinkExportException if an error occurs during datasink export
    */
   void exportIntoDatasink(Object report, GoogleDriveDatasink googleDriveDatasink, String filename, String folder)
         throws DatasinkExportException;

   /**
    * Issues a GoogleDrive test request by creating a simple text file and sending
    * it to the specified directory in the GoogleDrive of the datasink.
    * 
    * @param googleDriveDatasink the {@link GoogleDriveDatasink} to test
    * @throws DatasinkExportException if an error occurs during datasink export
    */
   void testDatasink(GoogleDriveDatasink googleDriveDatasink) throws DatasinkExportException;

   /**
    * Gets the default datasink configured in the datasinks.cf configuration file.
    * 
    * @return the default datasink
    */
   Optional<GoogleDriveDatasink> getDefaultDatasink();

}
