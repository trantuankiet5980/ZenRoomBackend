package vn.edu.iuh.fit.services.impl;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;
import vn.edu.iuh.fit.dtos.AddressDto;
import vn.edu.iuh.fit.services.GeocodingService;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class GeocodingServiceImpl implements GeocodingService {
    private final RestTemplate restTemplate;
    private final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    private final String apiKey = dotenv.get("GOOGLE_MAPS_API_KEY");

    public GeocodingServiceImpl() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public double[] getLatLngFromAddress(String fullAddress) {
        try {
            String url = "https://maps.googleapis.com/maps/api/geocode/json?address="
                    + URLEncoder.encode(fullAddress, StandardCharsets.UTF_8)
                    + "&key=" + apiKey;

            String response = restTemplate.getForObject(url, String.class);
            JSONObject json = new JSONObject(response);

            JSONArray results = json.getJSONArray("results");
            if (results.length() > 0) {
                JSONObject location = results.getJSONObject(0)
                        .getJSONObject("geometry")
                        .getJSONObject("location");
                double lat = location.getDouble("lat");
                double lng = location.getDouble("lng");
                return new double[]{lat, lng};
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

