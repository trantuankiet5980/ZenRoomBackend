package vn.edu.iuh.fit.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.PropertyDto;
import vn.edu.iuh.fit.dtos.requests.EventRequest;
import vn.edu.iuh.fit.services.EventService;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<Void> recordEvent(@Valid @RequestBody EventRequest request, Principal principal) {
        String userId = principal != null ? principal.getName() : null;
        eventService.recordEvent(userId, request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/recently-viewed")
    public ResponseEntity<List<PropertyDto>> getRecentlyViewed(
            @RequestParam(name = "limit", defaultValue = "6") int limit,
            Principal principal
    ) {
        if (principal == null) {
            return ResponseEntity.ok(List.of());
        }

        return ResponseEntity.ok(eventService.getRecentlyViewedProperties(principal.getName(), limit));
    }
}
