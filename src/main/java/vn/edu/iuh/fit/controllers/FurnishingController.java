package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.repositories.FurnishingRepository;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/furnishings")
public class FurnishingController {
    private final FurnishingRepository furnishingRepository;

    @GetMapping
    public ResponseEntity<?> listAll() {
        return ResponseEntity.ok(furnishingRepository.findAll());
    }
}
