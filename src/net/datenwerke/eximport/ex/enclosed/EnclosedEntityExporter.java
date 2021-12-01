package net.datenwerke.eximport.ex.enclosed;

import net.datenwerke.eximport.ExImportHelperService;
import net.datenwerke.eximport.ex.objectexporters.EntityObjectExporterFactory;

import com.google.inject.Inject;

/**
 * 
 *
 */
public class EnclosedEntityExporter extends EnclosedObjectExporter {

	private static final String EXPORTER_ID = "EnclosedEntityExporter";

	@Inject
	public EnclosedEntityExporter(
		EntityObjectExporterFactory exporterFactory,
		ExImportHelperService eiHelper
		){
		super(exporterFactory, eiHelper);
	}

	@Override
	public String getExporterId() {
		return EXPORTER_ID;
	}


}
