package vn.edu.iuh.fit.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
public class PostCreateDTO {
    private String landlordId;     // chủ nhà đăng
    private String propertyId;     // chọn phòng/tòa đã có
    private String title;          // tiêu đề
    private String description;    // mô tả
    private String contactPhone;   // SĐT liên hệ
    private Boolean isFireSafe;    // tick PCCC
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
}
