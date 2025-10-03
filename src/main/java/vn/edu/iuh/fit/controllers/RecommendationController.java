package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.dtos.PropertyDto;
import vn.edu.iuh.fit.services.RecommendationService;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/similar")
    public ResponseEntity<List<PropertyDto>> getSimilar(
            @RequestParam("roomId") String roomId,
            @RequestParam(name = "limit", defaultValue = "6") int limit,
            Principal principal
    ) {
        String userId = principal != null ? principal.getName() : null;
        return ResponseEntity.ok(recommendationService.getSimilarRooms(roomId, limit, userId));
    }

    @GetMapping("/personal")
    public ResponseEntity<List<PropertyDto>> getPersonal(
            @RequestParam(name = "limit", defaultValue = "20") int limit,
            Principal principal
    ) {
        String userId = principal != null ? principal.getName() : null;
        return ResponseEntity.ok(recommendationService.getPersonalRecommendations(userId, limit));
    }

    @GetMapping("/after-search")
    public ResponseEntity<List<PropertyDto>> rerankAfterSearch(
            @RequestParam("query") String query,
            @RequestParam(name = "limit", defaultValue = "20") int limit,
            Principal principal
    ) {
        String userId = principal != null ? principal.getName() : null;
        return ResponseEntity.ok(recommendationService.rerankAfterSearch(query, limit, userId));
    }
}
