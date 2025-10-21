package vn.edu.iuh.fit.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.dtos.chat.AIChatRequest;
import vn.edu.iuh.fit.dtos.chat.AiChatResponse;
import vn.edu.iuh.fit.dtos.chat.AiSuggestionRequest;
import vn.edu.iuh.fit.dtos.chat.AiSuggestionResponse;
import vn.edu.iuh.fit.services.AiChatService;
import vn.edu.iuh.fit.services.AiSuggestionService;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;
    private final AiSuggestionService aiSuggestionService;

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(@Valid @RequestBody AIChatRequest request) {
        return ResponseEntity.ok(aiChatService.chat(request));
    }

    @PostMapping("/suggestions")
    public ResponseEntity<AiSuggestionResponse> suggest(@Valid @RequestBody AiSuggestionRequest request) {
        return ResponseEntity.ok(aiSuggestionService.suggest(request));
    }
}
