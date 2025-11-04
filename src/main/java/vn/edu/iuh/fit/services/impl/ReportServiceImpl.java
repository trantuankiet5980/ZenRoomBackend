package vn.edu.iuh.fit.services.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.edu.iuh.fit.dtos.ReportDto;
import vn.edu.iuh.fit.dtos.requests.ReportCreateRequest;
import vn.edu.iuh.fit.entities.Property;
import vn.edu.iuh.fit.entities.Report;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.entities.enums.ReportStatus;
import vn.edu.iuh.fit.mappers.ReportMapper;
import vn.edu.iuh.fit.repositories.PropertyRepository;
import vn.edu.iuh.fit.repositories.ReportRepository;
import vn.edu.iuh.fit.services.AuthService;
import vn.edu.iuh.fit.services.ReportService;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final PropertyRepository propertyRepository;
    private final AuthService authService;
    private final ReportMapper reportMapper;

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
        return reportMapper.toDto(saved);
    }
}
