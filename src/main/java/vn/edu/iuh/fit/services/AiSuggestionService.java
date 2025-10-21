package vn.edu.iuh.fit.services;

import vn.edu.iuh.fit.dtos.chat.AiSuggestionRequest;
import vn.edu.iuh.fit.dtos.chat.AiSuggestionResponse;

public interface AiSuggestionService {
    AiSuggestionResponse suggest(AiSuggestionRequest request);
}
