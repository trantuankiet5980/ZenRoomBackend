package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.UserDto;
import vn.edu.iuh.fit.mappers.UserMapper;
import vn.edu.iuh.fit.services.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthInfoController {
    private final AuthService authService;
    private final UserMapper userMapper;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> me() {
        var user = authService.getCurrentUser();   // lấy từ SecurityContext + DB
        return ResponseEntity.ok(userMapper.toDto(user));
    }
}
