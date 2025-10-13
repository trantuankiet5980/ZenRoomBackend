package vn.edu.iuh.fit.services;

import org.springframework.web.multipart.MultipartFile;
import vn.edu.iuh.fit.entities.MessageAttachment;

import java.util.List;

public interface ChatAttachmentService {

    List<ChatService.AttachmentPayload> uploadImages(String senderId, List<MultipartFile> files);

    String resolveUrl(MessageAttachment attachment);
}
