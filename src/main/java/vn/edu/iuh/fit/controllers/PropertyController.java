package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.PropertyCreateDTO;
import vn.edu.iuh.fit.dtos.PropertyDto;
import vn.edu.iuh.fit.dtos.responses.ApiResponse;
import vn.edu.iuh.fit.entities.Property;
import vn.edu.iuh.fit.mappers.PropertyMapper;
import vn.edu.iuh.fit.services.PropertyService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;
    private final PropertyMapper propertyMapper;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody PropertyDto dto){
        try {
            Property createdProperty = propertyService.create(dto);
            PropertyDto propertyDto = propertyMapper.toDto(createdProperty);
            if (createdProperty == null) {
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(false)
                        .message("Property creation failed")
                        .data(null)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Property created successfully")
                    .data(propertyDto)
                    .build());
        } catch (IllegalArgumentException | jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
        }
    }

    @GetMapping("/landlord/{landlordId}")
    public ResponseEntity<?> getByLandlordId(@PathVariable String landlordId) {
        try {
            List<Property> properties = propertyService.getByLandlordId(landlordId);
            List<PropertyDto> propertyDtos = properties.stream()
                    .map(propertyMapper::toDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Properties retrieved successfully")
                    .data(propertyDtos)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
        }
    }
    @GetMapping("/all")
    public ResponseEntity<?> getAll() {
        try {
            List<Property> properties = propertyService.getAll();
            List<PropertyDto> propertyDtos = properties.stream()
                    .map(propertyMapper::toDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("All properties retrieved successfully")
                    .data(propertyDtos)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
        }
    }
}
