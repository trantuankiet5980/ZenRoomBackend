package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.entities.enums.ApartmentCategory;
import vn.edu.iuh.fit.entities.enums.ReportReason;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/enums")
public class EnumController {

    @GetMapping("/apartment-categories")
    public ApartmentCategory[] getApartmentCategories() {
        return ApartmentCategory.values();
    }

    @GetMapping("/report-reasons")
    public ReportReason[] getReportReasons() {
        return ReportReason.values();
    }
}
