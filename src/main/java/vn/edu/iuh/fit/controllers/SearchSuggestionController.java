package vn.edu.iuh.fit.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.SearchSuggestionDto;
import vn.edu.iuh.fit.dtos.requests.SearchInteractionRequest;
import vn.edu.iuh.fit.services.SearchSuggestionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search-suggestions")
@RequiredArgsConstructor
public class SearchSuggestionController {

    private final SearchSuggestionService searchSuggestionService;

    @GetMapping
    public ResponseEntity<List<SearchSuggestionDto>> suggest(
            @RequestParam("q") String query,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return ResponseEntity.ok(searchSuggestionService.suggest(query, limit));
    }

    @PostMapping("/rebuild/properties")
    public ResponseEntity<Void> rebuildPropertySuggestions() {
        searchSuggestionService.rebuildPropertySuggestions();
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/events")
    public ResponseEntity<Void> recordInteraction(@Valid @RequestBody SearchInteractionRequest request) {
        switch (request.getType()) {
            case QUERY -> searchSuggestionService.recordQuery(request.getQuery(), request.getSuggestionId());
            case CLICK -> searchSuggestionService.recordClick(request.getQuery(), request.getSuggestionId());
        }
        return ResponseEntity.accepted().build();
    }
}
