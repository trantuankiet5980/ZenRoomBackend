package vn.edu.iuh.fit.dtos.requests;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;
import vn.edu.iuh.fit.entities.enums.ReportReason;

@Getter
@Setter
public class ReportCreateRequest {
    @NotBlank
    private String propertyId;

    @NotNull
    private ReportReason reason;

    @Size(max = 1000)
    private String description;

    @AssertTrue(message = "Mô tả chi tiết là bắt buộc khi chọn lý do khác")
    public boolean isDescriptionValid() {
        if (reason == null) {
            return true;
        }
        if (reason == ReportReason.OTHER) {
            return StringUtils.hasText(description);
        }
        return true;
    }
}
