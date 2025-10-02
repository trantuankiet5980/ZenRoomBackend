package vn.edu.iuh.fit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.json.DistrictJson;
import vn.edu.iuh.fit.dtos.json.ProvinceJson;
import vn.edu.iuh.fit.dtos.json.WardJson;
import vn.edu.iuh.fit.entities.District;
import vn.edu.iuh.fit.entities.Province;
import vn.edu.iuh.fit.entities.Ward;
import vn.edu.iuh.fit.repositories.DistrictRepository;
import vn.edu.iuh.fit.repositories.ProvinceRepository;
import vn.edu.iuh.fit.repositories.WardRepository;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataImporter implements CommandLineRunner {

//    private final ProvinceRepository provinceRepository;
//    private final DistrictRepository districtRepository;
//    private final WardRepository wardRepository;
//
//    private final ObjectMapper objectMapper;
//
//    @Override
//    public void run(String... args) throws Exception {
//        importProvinces();
//        importDistricts();
//        importWards();
//    }
//
//    private void importProvinces() throws IOException {
//        InputStream inputStream = getClass().getResourceAsStream("/dist/tinh_tp.json");
//        // Đọc JSON thành Map<String, ProvinceJson>
//        Map<String, ProvinceJson> provincesMap = objectMapper.readValue(inputStream,
//                new TypeReference<Map<String, ProvinceJson>>() {});
//
//        for (ProvinceJson p : provincesMap.values()) {
//            Province province = new Province(p.getCode(), p.getName_with_type());
//            provinceRepository.save(province);
//        }
//    }
//
//
//    private void importDistricts() throws IOException {
//        InputStream inputStream = getClass().getResourceAsStream("/dist/quan_huyen.json");
//        // Đọc JSON thành Map<String, DistrictJson>
//        Map<String, DistrictJson> districtsMap = objectMapper.readValue(inputStream,
//                new TypeReference<Map<String, DistrictJson>>() {});
//
//        for (DistrictJson d : districtsMap.values()) {
//            Province province = provinceRepository.findById(d.getParent_code()).orElse(null);
//            if (province == null) continue;
//            District district = new District(d.getCode(), d.getName_with_type(), province);
//            districtRepository.save(district);
//        }
//    }
//
//
//    private void importWards() throws IOException {
//        InputStream inputStream = getClass().getResourceAsStream("/dist/xa_phuong.json");
//        // Đọc JSON thành Map<String, WardJson>
//        Map<String, WardJson> wardsMap = objectMapper.readValue(inputStream,
//                new TypeReference<Map<String, WardJson>>() {});
//
//        for (WardJson w : wardsMap.values()) {
//            District district = districtRepository.findById(w.getParent_code()).orElse(null);
//            if (district == null) continue;
//            Ward ward = new Ward(w.getCode(), w.getName_with_type(), district);
//            wardRepository.save(ward);
//        }
//    }

    @Override
    public void run(String... args) throws Exception {
        // Chạy import dữ liệu khi ứng dụng khởi động
    }

}