package vn.edu.iuh.fit.dtos.chat;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record AIChatRequest(
        @NotBlank String message,
        List<HistoryMessage> history,
        Integer limit
) {
    public int sanitizedLimit() {
        if (limit == null || limit < 1) {
            return 5;
        }
        return Math.min(limit, 10);
    }

    public record HistoryMessage(String role, String content) { }
}
