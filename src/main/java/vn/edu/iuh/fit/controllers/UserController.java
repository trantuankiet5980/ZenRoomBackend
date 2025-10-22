package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.UserDto;
import vn.edu.iuh.fit.dtos.responses.ApiResponse;
import vn.edu.iuh.fit.dtos.user.UserCreateRequest;
import vn.edu.iuh.fit.dtos.user.UserResponse;
import vn.edu.iuh.fit.dtos.user.UserUpdateRequest;
import vn.edu.iuh.fit.entities.enums.UserStatus;
import vn.edu.iuh.fit.services.AuthService;
import vn.edu.iuh.fit.services.FollowService;
import vn.edu.iuh.fit.services.UserService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FollowService followService;
    private final AuthService authService;

    @PostMapping
    public ResponseEntity<UserDto> create(@RequestBody UserCreateRequest user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(user));
    }
    @GetMapping
    public ResponseEntity<Page<UserResponse>> list(
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(name = "roles", required = false) List<String> roleNames,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(sortDirection);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid sortDirection. Use ASC or DESC");
        }

        String sortProperty = switch (sortBy) {
            case "createdAt" -> "createdAt";
            case "role" -> "role.roleName";
            case "status" -> "status";
            default -> throw new IllegalArgumentException("Invalid sortBy. Use createdAt, role or status");
        };

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate must be before toDate");
        }

        int safePage = Math.max(page, 0);
        int pageSize = Math.max(1, Math.min(size, 100));

        PageRequest pageRequest = PageRequest.of(safePage, pageSize, Sort.by(direction, sortProperty));
        LocalDateTime createdFrom = Optional.ofNullable(fromDate).map(LocalDate::atStartOfDay).orElse(null);
        LocalDateTime createdTo = Optional.ofNullable(toDate).map(date -> date.atTime(23, 59, 59)).orElse(null);

        return ResponseEntity.ok(userService.list(pageRequest, keyword, createdFrom, createdTo, roleNames, status));
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

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/follow")
    public ResponseEntity<?> follow(@PathVariable String id) {
        followService.follow(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Followed"));
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}/follow")
    public ResponseEntity<?> unfollow(@PathVariable String id) {
        followService.unfollow(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Unfollowed"));
    }

    @GetMapping("{id}/followers")
    public ResponseEntity<?> followers(@PathVariable String id,
                                       @RequestParam(defaultValue="0") int page,
                                       @RequestParam(defaultValue="20") int size) {
        var data = followService.listFollowers(id, PageRequest.of(page, Math.min(size, 100)));
        return ResponseEntity.ok(data);
    }

    @GetMapping("{id}/following")
    public ResponseEntity<?> following(@PathVariable String id,
                                       @RequestParam(defaultValue="0") int page,
                                       @RequestParam(defaultValue="20") int size) {
        var data = followService.listFollowing(id, PageRequest.of(page, Math.min(size, 100)));
        return ResponseEntity.ok(data);
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<?> stats(@PathVariable String id) {
        return ResponseEntity.ok(Map.of(
                "followers", followService.countFollowers(id),
                "following", followService.countFollowing(id)
        ));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/profile")
    public ResponseEntity<UserDto> getMyProfile() {
        return ResponseEntity.ok(userService.getById(authService.getCurrentUser().getUserId()));
    }

}
