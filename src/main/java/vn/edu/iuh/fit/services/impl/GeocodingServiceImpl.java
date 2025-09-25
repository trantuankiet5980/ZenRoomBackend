package vn.edu.iuh.fit.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;
import vn.edu.iuh.fit.dtos.AddressDto;
import vn.edu.iuh.fit.services.GeocodingService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

public class GeocodingServiceImpl implements GeocodingService {

    private final RestTemplate restTemplate;

    @Value("${google.maps.api.key}")
    private String apiKey;

    public GeocodingServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public AddressDto enrichAddress(AddressDto dto) {
        String address = String.join(", ",
                dto.getHouseNumber(),
                dto.getStreet(),
                dto.getWardName(),
                dto.getDistrictName(),
                dto.getProvinceName()
        );

        String url = "https://maps.googleapis.com/maps/api/geocode/json?address="
                + UriUtils.encodeQueryParam(address, StandardCharsets.UTF_8)
                + "&key=" + apiKey;

        JsonNode response = restTemplate.getForObject(url, JsonNode.class);

        if (response != null && response.has("results") && response.get("results").size() > 0) {
            JsonNode location = response.get("results").get(0).get("geometry").get("location");

            dto.setLatitude(BigDecimal.valueOf(location.get("lat").asDouble()));
            dto.setLongitude(BigDecimal.valueOf(location.get("lng").asDouble()));
            dto.setAddressFull(response.get("results").get(0).get("formatted_address").asText());
        }

        return dto;
    }
}
