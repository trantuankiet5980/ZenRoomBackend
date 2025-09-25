package vn.edu.iuh.fit.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import vn.edu.iuh.fit.dtos.AddressDto;
import vn.edu.iuh.fit.dtos.CoordinatesDTO;
import vn.edu.iuh.fit.entities.Address;
import vn.edu.iuh.fit.entities.District;
import vn.edu.iuh.fit.entities.Province;
import vn.edu.iuh.fit.entities.Ward;
import vn.edu.iuh.fit.mappers.AddressMapper;
import vn.edu.iuh.fit.repositories.AddressRepository;
import vn.edu.iuh.fit.repositories.DistrictRepository;
import vn.edu.iuh.fit.repositories.ProvinceRepository;
import vn.edu.iuh.fit.repositories.WardRepository;
import vn.edu.iuh.fit.services.AddressService;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    @Value("${google.maps.api.key}")
    private String apiKey;

    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;
    private final RestTemplate restTemplate = new RestTemplate();
    private final WardRepository wardRepository;
    private final DistrictRepository districtRepository;
    private final ProvinceRepository provinceRepository;

    @Override
    public CoordinatesDTO getCoordinatesFromFullAddress(String fullAddress) {
        try {
            if (fullAddress == null || fullAddress.isEmpty()) return null;

            // URL Google Maps API
            String url = "https://maps.googleapis.com/maps/api/geocode/json?address="
                    + URLEncoder.encode(fullAddress, StandardCharsets.UTF_8)
                    + "&key=" + apiKey;

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JSONObject json = new JSONObject(response.getBody());

            if ("OK".equals(json.optString("status"))) {
                JSONArray results = json.getJSONArray("results");
                if (results.length() > 0) {
                    JSONObject location = results.getJSONObject(0)
                            .getJSONObject("geometry")
                            .getJSONObject("location");

                    double lat = location.getDouble("lat");
                    double lng = location.getDouble("lng");

                    return new CoordinatesDTO(lat, lng);
                }
            }

            // Fallback sang OSM nếu Google fail
            String osmUrl = "https://nominatim.openstreetmap.org/search?format=json&q="
                    + URLEncoder.encode(fullAddress, StandardCharsets.UTF_8);

            ResponseEntity<String> osmResponse = restTemplate.getForEntity(osmUrl, String.class);
            JSONArray osmArr = new JSONArray(osmResponse.getBody());
            if (osmArr.length() > 0) {
                JSONObject obj = osmArr.getJSONObject(0);
                double lat = obj.getDouble("lat");
                double lng = obj.getDouble("lon");
                return new CoordinatesDTO(lat, lng);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    @Override
    public String getAddressFromCoordinates(double lat, double lng) {
        try {
            // Google Maps API
            String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng="
                    + lat + "," + lng
                    + "&key=" + apiKey;

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JSONObject json = new JSONObject(response.getBody());

            if ("OK".equals(json.optString("status"))) {
                JSONArray results = json.getJSONArray("results");
                if (results.length() > 0) {
                    return results.getJSONObject(0).getString("formatted_address");
                }
            }

            // Nếu Google fail thì fallback sang OSM
            String osmUrl = "https://nominatim.openstreetmap.org/reverse?format=json&lat="
                    + lat + "&lon=" + lng;

            ResponseEntity<String> osmResponse = restTemplate.getForEntity(osmUrl, String.class);
            JSONObject osmJson = new JSONObject(osmResponse.getBody());

            if (osmJson.has("display_name")) {
                return osmJson.getString("display_name");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public AddressDto save(AddressDto dto) {
        try {
            // 1. Tìm Ward
            if (dto.getWardId() == null) {
                throw new RuntimeException("Ward ID is required.");
            }

            Ward ward = wardRepository.findById(dto.getWardId())
                    .orElseThrow(() -> new RuntimeException("Ward not found"));
            District district = ward.getDistrict();
            Province province = district.getProvince();

            // 2. Tạo addressFull nếu chưa có
            String addressFull = String.join(", ",
                    dto.getHouseNumber() != null ? dto.getHouseNumber() : "",
                    dto.getStreet() != null ? dto.getStreet() : "",
                    ward.getName(),
                    district.getName(),
                    province.getName()
            );
            dto.setAddressFull(addressFull);

            // 3. Lấy tọa độ nếu chưa có
            if (dto.getLatitude() == null || dto.getLongitude() == null) {
                CoordinatesDTO coords = getCoordinatesFromFullAddress(addressFull);
                if (coords != null) {
                    dto.setLatitude(BigDecimal.valueOf(coords.getLatitude()));
                    dto.setLongitude(BigDecimal.valueOf(coords.getLongitude()));

                    // Debug
                    System.out.println("Address: " + addressFull);
                    System.out.println("Latitude: " + coords.getLatitude() + ", Longitude: " + coords.getLongitude());
                }
            }

            // 4. Map DTO -> Entity
            Address entity = addressMapper.toEntity(dto);
            entity.setWard(ward);
            entity.setDistrict(district);
            entity.setProvince(province);

            // 5. Gán tọa độ vào entity
            entity.setLatitude(dto.getLatitude());
            entity.setLongitude(dto.getLongitude());

            // 6. Tạo addressFull cho entity nếu chưa có
            if (entity.getAddressFull() == null || entity.getAddressFull().isEmpty()) {
                entity.generateAddressFull();
            }

            Address saved = addressRepository.save(entity);

            return addressMapper.toDto(saved);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to save address: " + e.getMessage());
        }
    }



    @Override
    public AddressDto update(String id, AddressDto dto) {
        Address entity = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        addressMapper.updateEntity(entity, dto);
        return addressMapper.toDto(addressRepository.save(entity));
    }

    @Override
    public AddressDto getById(String id) {
        return addressRepository.findById(id)
                .map(addressMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Address not found"));
    }

    @Override
    public List<AddressDto> getAll() {
        return addressRepository.findAll()
                .stream().map(addressMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(String id) {
        addressRepository.deleteById(id);
    }
}
