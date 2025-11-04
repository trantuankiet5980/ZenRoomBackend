package vn.edu.iuh.fit.services;

import vn.edu.iuh.fit.dtos.ReportDto;
import vn.edu.iuh.fit.dtos.requests.ReportCreateRequest;

public interface ReportService {
    ReportDto reportProperty(ReportCreateRequest request);
}
