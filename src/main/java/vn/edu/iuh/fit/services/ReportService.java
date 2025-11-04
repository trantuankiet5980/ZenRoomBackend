package vn.edu.iuh.fit.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.edu.iuh.fit.dtos.ReportDto;
import vn.edu.iuh.fit.dtos.ReportListItemDto;
import vn.edu.iuh.fit.dtos.requests.ReportCreateRequest;

import java.time.LocalDate;

public interface ReportService {
    ReportDto reportProperty(ReportCreateRequest request);

    Page<ReportListItemDto> listReports(LocalDate fromDate, LocalDate toDate, Pageable pageable);
}
