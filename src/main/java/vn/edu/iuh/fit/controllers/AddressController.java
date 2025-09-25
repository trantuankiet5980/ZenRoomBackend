package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.AddressDto;
import vn.edu.iuh.fit.dtos.CoordinatesDTO;
import vn.edu.iuh.fit.services.AddressService;
import vn.edu.iuh.fit.dtos.responses.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping("/coordinates")
    public ApiResponse<CoordinatesDTO> getCoordinates(@RequestParam String address) {
        CoordinatesDTO coordinates = addressService.getCoordinatesFromFullAddress(address);
        return ApiResponse.<CoordinatesDTO>builder()
                .success(coordinates != null)
                .message(coordinates != null ? "Coordinates fetched successfully" : "Coordinates not found")
                .data(coordinates)
                .build();
    }

    @GetMapping("/reverse")
    public ApiResponse<String> getAddress(@RequestParam double lat, @RequestParam double lng) {
        String address = addressService.getAddressFromCoordinates(lat, lng);
        return ApiResponse.<String>builder()
                .success(address != null)
                .message(address != null ? "Address fetched successfully" : "Address not found")
                .data(address)
                .build();
    }

    @PostMapping
    public ApiResponse<AddressDto> create(@RequestBody AddressDto dto) {
        AddressDto saved = addressService.save(dto);
        return ApiResponse.<AddressDto>builder()
                .success(true)
                .message("Address created successfully")
                .data(saved)
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<AddressDto> update(@PathVariable String id, @RequestBody AddressDto dto) {
        AddressDto updated = addressService.update(id, dto);
        return ApiResponse.<AddressDto>builder()
                .success(true)
                .message("Address updated successfully")
                .data(updated)
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<AddressDto> getById(@PathVariable String id) {
        AddressDto address = addressService.getById(id);
        return ApiResponse.<AddressDto>builder()
                .success(address != null)
                .message(address != null ? "Address fetched successfully" : "Address not found")
                .data(address)
                .build();
    }

    @GetMapping
    public ApiResponse<List<AddressDto>> getAll() {
        List<AddressDto> addresses = addressService.getAll();
        return ApiResponse.<List<AddressDto>>builder()
                .success(true)
                .message("Addresses fetched successfully")
                .data(addresses)
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        addressService.delete(id);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Address deleted successfully")
                .build();
    }
}