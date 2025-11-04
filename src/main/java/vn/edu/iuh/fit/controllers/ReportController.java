package vn.edu.iuh.fit.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.dtos.ReportDto;
import vn.edu.iuh.fit.dtos.requests.ReportCreateRequest;
import vn.edu.iuh.fit.dtos.responses.ApiResponse;
import vn.edu.iuh.fit.services.ReportService;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

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
