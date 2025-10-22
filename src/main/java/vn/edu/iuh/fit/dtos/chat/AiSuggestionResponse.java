package vn.edu.iuh.fit.dtos.chat;

import java.util.List;

public record AiSuggestionResponse(List<SuggestionItem> suggestions) {
    public AiSuggestionResponse {
        suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
    }

    public record SuggestionItem(String text) { }
}
