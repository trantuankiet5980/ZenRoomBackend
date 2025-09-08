package vn.edu.iuh.fit.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PostRejectRequest {
    @NotBlank(message = "Reason for rejection must not be blank")
    private String reason;
}
