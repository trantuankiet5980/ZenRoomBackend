package vn.edu.iuh.fit.dtos.chat;

import java.util.List;

public record AiChatResponse(
        String reply,
        ChatFilterDto filters,
        List<ChatPropertyDto> results
) {
}
