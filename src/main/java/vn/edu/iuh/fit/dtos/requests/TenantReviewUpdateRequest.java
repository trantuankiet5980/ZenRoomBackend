package vn.edu.iuh.fit.dtos.requests;

import lombok.Data;

@Data
public class TenantReviewUpdateRequest {
    private String tenantReviewId;
    private Integer rating;   // 1..5
    private String comment;
}
