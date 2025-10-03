package vn.edu.iuh.fit.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.dtos.requests.EventRequest;
import vn.edu.iuh.fit.services.EventService;

import java.security.Principal;

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
}
