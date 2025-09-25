package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.*;
import vn.edu.iuh.fit.services.AdministrativeService;
import vn.edu.iuh.fit.dtos.responses.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/administrative")
@RequiredArgsConstructor
public class AdministrativeController {
    private final AdministrativeService service;

    @GetMapping("/provinces")
    public ApiResponse<List<ProvinceDto>> getAllProvinces() {
        List<ProvinceDto> provinces = service.getAllProvinces();
        return ApiResponse.<List<ProvinceDto>>builder()
                .success(true)
                .message("Provinces fetched successfully")
                .data(provinces)
                .build();
    }

    @GetMapping("/districts/{provinceCode}")
    public ApiResponse<List<DistrictDto>> getDistricts(@PathVariable String provinceCode) {
        List<DistrictDto> districts = service.getDistrictsByProvince(provinceCode);
        return ApiResponse.<List<DistrictDto>>builder()
                .success(true)
                .message("Districts fetched successfully")
                .data(districts)
                .build();
    }

    @GetMapping("/wards/{districtCode}")
    public ApiResponse<List<WardDto>> getWards(@PathVariable String districtCode) {
        List<WardDto> wards = service.getWardsByDistrict(districtCode);
        return ApiResponse.<List<WardDto>>builder()
                .success(true)
                .message("Wards fetched successfully")
                .data(wards)
                .build();
    }
}