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
import vn.edu.iuh.fit.entities.Ward;
import vn.edu.iuh.fit.mappers.AddressMapper;
import vn.edu.iuh.fit.repositories.AddressRepository;
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

    @Override
    public CoordinatesDTO getCoordinatesFromFullAddress(String fullAddress) {
        try {
            if (fullAddress == null || fullAddress.isEmpty()) return null;

            // Google Maps API
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

            // Fallback sang OSM
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

            // Fallback sang OSM
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
            if (dto.getWardId() == null) {
                throw new RuntimeException("Ward ID is required.");
            }

            Ward ward = wardRepository.findById(dto.getWardId())
                    .orElseThrow(() -> new RuntimeException("Ward not found"));

            // Build addressFull nếu DTO chưa có
            String fullAddress = dto.getAddressFull();
            if (fullAddress == null || fullAddress.isBlank()) {
                fullAddress = String.join(", ",
                        dto.getHouseNumber() != null ? dto.getHouseNumber() : "",
                        dto.getStreet() != null ? dto.getStreet() : "",
                        ward.getName_with_type(),
                        ward.getDistrict().getName_with_type(),
                        ward.getDistrict().getProvince().getName_with_type()
                );
                dto.setAddressFull(fullAddress);
            }

            if (dto.getLatitude() == null || dto.getLongitude() == null) {
                CoordinatesDTO coords = getCoordinatesFromFullAddress(fullAddress);
                if (coords != null) {
                    dto.setLatitude(BigDecimal.valueOf(coords.getLatitude()));
                    dto.setLongitude(BigDecimal.valueOf(coords.getLongitude()));
                }
            }


            // Map DTO -> Entity
            Address entity = addressMapper.toEntity(dto, ward);

            // Save DB
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

        // Nếu đổi wardId thì load ward mới
        Ward ward = dto.getWardId() != null
                ? wardRepository.findById(dto.getWardId())
                .orElseThrow(() -> new RuntimeException("Ward not found"))
                : entity.getWard();

        // Update entity từ DTO
        addressMapper.updateEntity(entity, dto, ward);

        // Nếu chưa có lat/lng thì gọi API để bổ sung
        if ((entity.getLatitude() == null || entity.getLongitude() == null)
                && entity.getAddressFull() != null) {
            CoordinatesDTO coords = getCoordinatesFromFullAddress(entity.getAddressFull());
            if (coords != null) {
                entity.setLatitude(BigDecimal.valueOf(coords.getLatitude()));
                entity.setLongitude(BigDecimal.valueOf(coords.getLongitude()));
            }
        }

        Address saved = addressRepository.save(entity);
        return addressMapper.toDto(saved);
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
