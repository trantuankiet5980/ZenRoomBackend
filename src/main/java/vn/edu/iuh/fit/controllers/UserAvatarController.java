package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.iuh.fit.services.AvatarService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserAvatarController {

    private final AvatarService avatarService;

    /** User tự đổi avatar của mình */
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadMyAvatar(@RequestPart("file") MultipartFile file) {
        String url = avatarService.uploadMyAvatar(file);
        return ResponseEntity.ok().body(new UploadAvatarResponse(url));
    }

    /** Admin đổi avatar cho user khác */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{userId}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAvatarForUser(@PathVariable String userId,
                                                 @RequestPart("file") MultipartFile file) {
        String url = avatarService.uploadAvatarForUser(userId, file);
        return ResponseEntity.ok().body(new UploadAvatarResponse(url));
    }

    /** Lấy presigned URL (nếu bucket private) cho avatar hiện tại */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me/avatar/presign")
    public ResponseEntity<?> presignMyAvatar(@RequestParam(defaultValue = "15") long minutes) {
        String url = avatarService.presignMyAvatar(minutes);
        return ResponseEntity.ok().body(new UploadAvatarResponse(url));
    }

    /* ---- small DTO ---- */
    private record UploadAvatarResponse(String url) {}
}
