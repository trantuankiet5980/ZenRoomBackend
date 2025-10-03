package vn.edu.iuh.fit.services.embedding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.entities.Address;
import vn.edu.iuh.fit.entities.Property;
import vn.edu.iuh.fit.entities.PropertyFurnishing;
import vn.edu.iuh.fit.entities.PropertyServiceItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class PythonPropertyEmbeddingGenerator implements PropertyEmbeddingGenerator {

    private final ObjectMapper objectMapper;

    @Value("${embedding.python.command:python3}")
    private String pythonCommand;

    @Value("${embedding.python.script:./scripts/generate_property_embedding.py}")
    private String scriptPath;

    @Value("${embedding.python.timeout-seconds:60}")
    private long timeoutSeconds;

    @Override
    public Optional<double[]> generate(Property property) {
        if (property == null) {
            return Optional.empty();
        }

        Map<String, Object> payload = buildPayload(property);
        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize property {} for embedding generation: {}", property.getPropertyId(), ex.getMessage());
            return Optional.empty();
        }

        Path script = Path.of(scriptPath);
        if (!script.toFile().exists()) {
            log.warn("Embedding script not found at {}", script.toAbsolutePath());
            return Optional.empty();
        }

        ProcessBuilder processBuilder = new ProcessBuilder(pythonCommand, script.toAbsolutePath().toString());
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            writeRequest(process, requestBody);
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                log.warn("Embedding generator timed out for property {}", property.getPropertyId());
                return Optional.empty();
            }
            String output = readOutput(process.getInputStream());
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.warn("Embedding generator exited with code {} for property {}. Output: {}", exitCode, property.getPropertyId(), output);
                return Optional.empty();
            }
            if (output == null || output.isBlank()) {
                log.warn("Embedding generator returned empty payload for property {}", property.getPropertyId());
                return Optional.empty();
            }
            EmbeddingResponse response = objectMapper.readValue(output, EmbeddingResponse.class);
            double[] embedding = response.embedding();
            if (embedding == null || embedding.length == 0) {
                log.warn("Embedding generator produced no vector for property {}", property.getPropertyId());
                return Optional.empty();
            }
            return Optional.of(embedding);
        } catch (IOException ex) {
            log.warn("Failed to execute embedding generator for property {}: {}", property.getPropertyId(), ex.getMessage());
            return Optional.empty();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Embedding generator interrupted for property {}", property.getPropertyId());
            return Optional.empty();
        }
    }

    private void writeRequest(Process process, String payload) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(payload);
            writer.flush();
        }
    }

    private String readOutput(InputStream inputStream) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private Map<String, Object> buildPayload(Property property) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("propertyId", property.getPropertyId());
        payload.put("title", safe(property.getTitle()));
        payload.put("description", safe(property.getDescription()));
        payload.put("buildingName", safe(property.getBuildingName()));
        payload.put("propertyType", property.getPropertyType() != null ? property.getPropertyType().name() : null);
        payload.put("apartmentCategory", property.getApartmentCategory() != null ? property.getApartmentCategory().name() : null);
        payload.put("area", property.getArea());
        payload.put("price", property.getPrice());
        payload.put("deposit", property.getDeposit());
        payload.put("capacity", property.getCapacity());
        payload.put("bedrooms", property.getBedrooms());
        payload.put("bathrooms", property.getBathrooms());
        payload.put("floorNo", property.getFloorNo());
        payload.put("parkingSlots", property.getParkingSlots());
        payload.put("roomNumber", safe(property.getRoomNumber()));
        payload.put("address", buildAddress(property.getAddress()));
        payload.put("services", buildServices(property.getServices()));
        payload.put("furnishings", buildFurnishings(property.getFurnishings()));
        return payload;
    }

    private Map<String, Object> buildAddress(Address address) {
        if (address == null) {
            return null;
        }
        Map<String, Object> addressMap = new HashMap<>();
        addressMap.put("houseNumber", safe(address.getHouseNumber()));
        addressMap.put("street", safe(address.getStreet()));
        addressMap.put("addressFull", safe(address.getAddressFull()));
        addressMap.put("ward", address.getWard() != null ? safe(address.getWard().getName_with_type()) : null);
        addressMap.put("district", address.getDistrict() != null ? safe(address.getDistrict().getName_with_type()) : null);
        addressMap.put("province", address.getProvince() != null ? safe(address.getProvince().getName_with_type()) : null);
        return addressMap;
    }

    private List<Map<String, Object>> buildServices(List<PropertyServiceItem> services) {
        if (services == null || services.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (PropertyServiceItem service : services) {
            Map<String, Object> item = new HashMap<>();
            item.put("serviceName", safe(service.getServiceName()));
            item.put("note", safe(service.getNote()));
            item.put("fee", service.getFee());
            item.put("chargeBasis", service.getChargeBasis() != null ? service.getChargeBasis().name() : null);
            item.put("included", service.getIsIncluded());
            items.add(item);
        }
        return items;
    }

    private List<Map<String, Object>> buildFurnishings(List<PropertyFurnishing> furnishings) {
        if (furnishings == null || furnishings.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (PropertyFurnishing furnishing : furnishings) {
            Map<String, Object> item = new HashMap<>();
            item.put("quantity", furnishing.getQuantity());
            if (furnishing.getFurnishing() != null) {
                item.put("name", safe(furnishing.getFurnishing().getFurnishingName()));
            }
            items.add(item);
        }
        return items;
    }

    private String safe(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EmbeddingResponse(double[] embedding, String error) {
    }
}
