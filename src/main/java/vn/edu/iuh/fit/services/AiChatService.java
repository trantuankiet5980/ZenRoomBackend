package vn.edu.iuh.fit.services;

import vn.edu.iuh.fit.dtos.chat.AIChatRequest;
import vn.edu.iuh.fit.dtos.chat.AiChatResponse;

public interface AiChatService {
    AiChatResponse chat(AIChatRequest request);
}
