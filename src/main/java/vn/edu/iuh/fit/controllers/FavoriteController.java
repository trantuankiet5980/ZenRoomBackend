package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.FavoriteDto;
import vn.edu.iuh.fit.dtos.requests.FavoriteRequest;
import vn.edu.iuh.fit.dtos.responses.FavoriteListResponse;
import vn.edu.iuh.fit.services.FavoriteService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping
    public ResponseEntity<FavoriteDto> addFavorite(@RequestBody FavoriteRequest request) {
        return ResponseEntity.ok(favoriteService.addFavorite(request));
    }

    @GetMapping
    public ResponseEntity<FavoriteListResponse> getFavorites() {
        List<FavoriteDto> favorites = favoriteService.getFavorites();
        return ResponseEntity.ok(new FavoriteListResponse(favorites));
    }

    @DeleteMapping("/{propertyId}")
    public ResponseEntity<Void> removeFavorite(@PathVariable String propertyId) {
        favoriteService.removeFavorite(propertyId);
        return ResponseEntity.noContent().build();
    }
}
