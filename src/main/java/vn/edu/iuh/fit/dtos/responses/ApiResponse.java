package vn.edu.iuh.fit.dtos.responses;

import com.google.auto.value.AutoValue;
import com.google.cloud.BatchResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

}
