package vn.edu.iuh.fit.dtos;

import lombok.Value;
import vn.edu.iuh.fit.entities.enums.MediaType;

import java.io.Serializable;
import java.time.LocalDateTime;

@Value
public class MessageAttachmentDto implements Serializable {
    String attachmentId;
    MediaType mediaType;
    String url;
    String contentType;
    Long size;
    LocalDateTime createdAt;
}
