package vn.edu.iuh.fit.dtos.requests;

import lombok.Data;

@Data
public class LoginRequest {
    private String phoneNumber;
    private String password;
}
