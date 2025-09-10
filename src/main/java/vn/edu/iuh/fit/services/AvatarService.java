package vn.edu.iuh.fit.services;

import org.springframework.web.multipart.MultipartFile;

public interface AvatarService {
    /** Upload avatar cho user hiện tại, cập nhật DB, trả về URL (public hoặc presigned). */
    String uploadMyAvatar(MultipartFile file);

    /** Admin đổi avatar cho user khác. */
    String uploadAvatarForUser(String userId, MultipartFile file);

    /** Tạo presigned URL (nếu bucket private) cho avatar hiện tại. */
    String presignMyAvatar(long minutesTtl);
}
