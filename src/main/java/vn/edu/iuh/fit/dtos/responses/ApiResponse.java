package vn.edu.iuh.fit.dtos.responses;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(String msg) {
        return ApiResponse.<T>builder().success(true).message(msg).build();
    }

    public static <T> ApiResponse<T> success(String msg, T data) {
        return ApiResponse.<T>builder().success(true).message(msg).data(data).build();
    }

    public static <T> ApiResponse<T> error(String msg) {
        return ApiResponse.<T>builder().success(false).message(msg).build();
    }

    public static <T> ApiResponse<T> error(String msg, T data) {
        return ApiResponse.<T>builder().success(false).message(msg).data(data).build();
    }
}