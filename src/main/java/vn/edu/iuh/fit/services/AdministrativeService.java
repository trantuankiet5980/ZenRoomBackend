package vn.edu.iuh.fit.services;

import vn.edu.iuh.fit.dtos.*;

import java.util.List;

public interface AdministrativeService {
    List<ProvinceDto> getAllProvinces();
    List<DistrictDto> getDistrictsByProvince(String provinceCode);
    List<WardDto> getWardsByDistrict(String districtCode);
}
