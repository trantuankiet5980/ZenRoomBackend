package vn.edu.iuh.fit.services.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.edu.iuh.fit.dtos.ReportDto;
import vn.edu.iuh.fit.dtos.ReportListItemDto;
import vn.edu.iuh.fit.dtos.requests.ReportCreateRequest;
import vn.edu.iuh.fit.entities.Property;
import vn.edu.iuh.fit.entities.Report;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.entities.enums.ReportStatus;
import vn.edu.iuh.fit.mappers.ReportMapper;
import vn.edu.iuh.fit.repositories.PropertyRepository;
import vn.edu.iuh.fit.repositories.ReportRepository;
import vn.edu.iuh.fit.services.AuthService;
import vn.edu.iuh.fit.services.RealtimeNotificationService;
import vn.edu.iuh.fit.services.ReportService;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final PropertyRepository propertyRepository;
    private final AuthService authService;
    private final ReportMapper reportMapper;
    private final RealtimeNotificationService realtimeNotificationService;

    @Override
    public ReportDto reportProperty(ReportCreateRequest request) {
        User reporter = authService.getCurrentUser();
        Property property = propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new EntityNotFoundException("Property not found: " + request.getPropertyId()));

        Report report = Report.builder()
                .reporter(reporter)
                .property(property)
                .reason(request.getReason())
                .description(StringUtils.hasText(request.getDescription())
                        ? request.getDescription().trim()
                        : null)
                .status(ReportStatus.PENDING)
                .build();

        Report saved = reportRepository.save(report);
        String reporterName = StringUtils.hasText(reporter.getFullName()) ? reporter.getFullName() : reporter.getEmail();
        String propertyTitle = property.getTitle();
        realtimeNotificationService.notifyAdminsReportCreated(reporterName, property.getPropertyId(), propertyTitle);
        return reportMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportListItemDto> listReports(LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        Specification<Report> spec = Specification.where(null);

        if (fromDate != null) {
            LocalDateTime fromDateTime = fromDate.atStartOfDay();
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), fromDateTime));
        }

        if (toDate != null) {
            LocalDateTime toDateTime = toDate.plusDays(1).atStartOfDay();
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("createdAt"), toDateTime));
        }

        Page<Report> page = reportRepository.findAll(spec, pageable);
        return page.map(reportMapper::toListItem);
    }
}
