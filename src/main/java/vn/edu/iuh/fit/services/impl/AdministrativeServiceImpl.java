package vn.edu.iuh.fit.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.dtos.*;
import vn.edu.iuh.fit.entities.*;
import vn.edu.iuh.fit.repositories.*;
import vn.edu.iuh.fit.services.AdministrativeService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdministrativeServiceImpl implements AdministrativeService {
    private final ProvinceRepository provinceRepo;
    private final DistrictRepository districtRepo;
    private final WardRepository wardRepo;

    @Override
    public List<ProvinceDto> getAllProvinces() {
        return provinceRepo.findAll().stream()
                .map(p -> new ProvinceDto(p.getCode(), p.getName_with_type()))
                .collect(Collectors.toList());
    }

    @Override
    public List<DistrictDto> getDistrictsByProvince(String provinceCode) {
        return districtRepo.findByProvince_Code(provinceCode).stream()
                .map(d -> new DistrictDto(d.getCode(), d.getName_with_type(), d.getProvince().getCode()))
                .collect(Collectors.toList());
    }

    @Override
    public List<WardDto> getWardsByDistrict(String districtCode) {
        return wardRepo.findByDistrict_Code(districtCode).stream()
                .map(w -> new WardDto(w.getCode(), w.getName_with_type(), w.getDistrict().getCode()))
                .collect(Collectors.toList());
    }
}