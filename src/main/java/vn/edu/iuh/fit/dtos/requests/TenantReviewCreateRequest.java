package vn.edu.iuh.fit.dtos.requests;

import lombok.Data;

@Data
public class TenantReviewCreateRequest {
    private String bookingId;
    private Integer rating;   // 1..5
    private String comment;
}
