package vn.edu.iuh.fit.mappers;

import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.ReportDto;
import vn.edu.iuh.fit.dtos.ReportListItemDto;
import vn.edu.iuh.fit.entities.Report;

@Component
public class ReportMapper {

    /** Entity -> DTO */
    public ReportDto toDto(Report e) {
        if (e == null) return null;
        return new ReportDto(
                e.getReportId(),
                e.getReporter() != null ? e.getReporter().getUserId() : null,
                e.getProperty() != null ? e.getProperty().getPropertyId() : null,
                e.getReason(),
                e.getDescription(),
                e.getStatus(),
                e.getCreatedAt()
        );
    }

    public ReportListItemDto toListItem(Report e) {
        if (e == null) return null;
        return new ReportListItemDto(
                e.getReportId(),
                e.getReporter() != null ? e.getReporter().getUserId() : null,
                e.getReporter() != null ? e.getReporter().getFullName() : null,
                e.getProperty() != null ? e.getProperty().getPropertyId() : null,
                e.getProperty() != null ? e.getProperty().getTitle() : null,
                e.getReason() != null ? e.getReason().getDescription() : null,
                e.getDescription(),
                e.getStatus(),
                e.getCreatedAt()
        );
    }

    /** DTO -> Entity (KHÔNG gắn reporter/property ở đây; gắn trong Service) */    public Report toEntity(ReportDto d) {
        if (d == null) return null;
        return Report.builder()
                .reportId(d.getReportId())
                .reason(d.getReason())
                .description(d.getDescription())
                .status(d.getStatus())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
