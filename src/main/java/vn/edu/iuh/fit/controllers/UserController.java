package vn.edu.iuh.fit.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.UserDto;
import vn.edu.iuh.fit.dtos.responses.ApiResponse;
import vn.edu.iuh.fit.dtos.user.UserCreateRequest;
import vn.edu.iuh.fit.dtos.user.UserResponse;
import vn.edu.iuh.fit.dtos.user.UserUpdateRequest;
import vn.edu.iuh.fit.services.UserService;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }
    @PostMapping
    public ResponseEntity<UserDto> create(@RequestBody UserCreateRequest user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(user));
    }
    @GetMapping
    public ResponseEntity<Page<UserResponse>> list(
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size){
        return ResponseEntity.ok(userService.list(PageRequest.of(page,size)));
    }

    /** Cập nhật hồ sơ của chính mình bằng UserDto (partial update) */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/profile")
    public ResponseEntity<UserDto> updateMyProfile(@RequestBody UserDto dto) {
        return ResponseEntity.ok(userService.updateMe(dto));
    }

    /** Lấy thông tin user bất kỳ theo id */
    @GetMapping("{id}")
    public ResponseEntity<UserDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    /** Admin cập nhật user bất kỳ bằng UserDto (partial update) */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("{id}")
    public ResponseEntity<UserDto> update(@PathVariable String id, @RequestBody UserDto dto) {
        return ResponseEntity.ok(userService.update(id, dto));
    }

    @PostMapping("/request-delete")
    public ResponseEntity<Void> requestDeleteAccount(@RequestParam String userId) {
        userService.requestDeleteAccount(userId);
        return ResponseEntity.ok().build();
    }
}
