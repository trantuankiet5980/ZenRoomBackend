package vn.edu.iuh.fit.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.ReportDto;
import vn.edu.iuh.fit.dtos.ReportListItemDto;
import vn.edu.iuh.fit.dtos.requests.ReportCreateRequest;
import vn.edu.iuh.fit.dtos.responses.ApiResponse;
import vn.edu.iuh.fit.dtos.responses.PageResponse;
import vn.edu.iuh.fit.services.ReportService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReportListItemDto>>> listReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "DESC") Sort.Direction sort,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = size > 0 ? size : 20;
        Sort.Direction direction = sort != null ? sort : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(resolvedPage, resolvedSize, Sort.by(direction, "createdAt"));

        PageResponse<ReportListItemDto> data = PageResponse.of(reportService.listReports(fromDate, toDate, pageable));
        ApiResponse<PageResponse<ReportListItemDto>> response = ApiResponse.<PageResponse<ReportListItemDto>>builder()
                .success(true)
                .data(data)
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReportDto>> reportProperty(@Valid @RequestBody ReportCreateRequest request) {
        ReportDto reportDto = reportService.reportProperty(request);
        ApiResponse<ReportDto> response = ApiResponse.<ReportDto>builder()
                .success(true)
                .message("Tạo báo cáo thành công")
                .data(reportDto)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
