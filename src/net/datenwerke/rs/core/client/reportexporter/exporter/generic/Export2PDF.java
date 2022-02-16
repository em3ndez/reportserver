package net.datenwerke.rs.core.client.reportexporter.exporter.generic;

import com.google.gwt.resources.client.ImageResource;

import net.datenwerke.rs.core.client.datasinkmanager.dto.DatasinkDefinitionDto;
import net.datenwerke.rs.core.client.reportexporter.exporter.ReportExporterImpl;
import net.datenwerke.rs.core.client.reportexporter.locale.ReportExporterMessages;
import net.datenwerke.rs.core.client.reportmanager.dto.reports.ReportDto;
import net.datenwerke.rs.tabledatasink.client.tabledatasink.dto.TableDatasinkDto;
import net.datenwerke.rs.theme.client.icon.BaseIcon;

public abstract class Export2PDF extends ReportExporterImpl {

   @Override
   public boolean consumesConfiguration(ReportDto report) {
      return true;
   }

   @Override
   public String getExportDescription() {
      return ReportExporterMessages.INSTANCE.Export2PDF();
   }

   @Override
   public String getExportTitle() {
      return "PDF"; //$NON-NLS-1$
   }

   @Override
   public String getOutputFormat() {
      return "PDF"; //$NON-NLS-1$
   }

   @Override
   public ImageResource getIcon() {
      return BaseIcon.fromFileExtension("pdf").toImageResource();
   }

   @Override
   public boolean hasConfiguration() {
      return false;
   }
   
   @Override
   public boolean supportsDatasink(Class<? extends DatasinkDefinitionDto> datasinkType) {
      return ! datasinkType.equals(TableDatasinkDto.class);
   }

}