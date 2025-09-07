package vn.edu.iuh.fit.dtos;

import lombok.Data;

@Data
public class PostCreateDTO {
    private String landlordId;     // chủ nhà đăng
    private String propertyId;     // chọn phòng/tòa đã có
    private String title;          // tiêu đề
    private String description;    // mô tả
    private String contactPhone;   // SĐT liên hệ
    private Boolean isFireSafe;    // tick PCCC
}
