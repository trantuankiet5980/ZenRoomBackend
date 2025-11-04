package vn.edu.iuh.fit.entities.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportReason {
    INAPPROPRIATE_CONTENT("Bài đăng chứa nội dung không phù hợp, nhạy cảm hoặc phản cảm"),
    FRAUD_OR_SCAM("Có dấu hiệu lừa đảo, giả mạo thông tin"),
    SPAM_OR_ADVERTISING("Bài đăng spam, trùng lặp hoặc quảng cáo không liên quan đến nền tảng."),
    MISLEADING_INFORMATION("Cung cấp thông tin sai lệch, không trung thực về chỗ ở, giá, tiện nghi, vị trí,…"),
    COPYRIGHT_VIOLATION("Vi phạm bản quyền hình ảnh, nội dung, thương hiệu của người khác."),
    HARASSMENT_OR_DISCRIMINATION("Bài đăng có phát ngôn phân biệt đối xử, xúc phạm hoặc quấy rối người khác."),
    UNSAFE_OR_DANGEROUS("Bài đăng mô tả chỗ ở hoặc hoạt động nguy hiểm, không đảm bảo an toàn."),
    FAKE_OR_NON_EXISTENT("Tin giả hoặc không tồn tại thực tế, hình ảnh lấy từ nơi khác."),
    OTHER("Lý do khác");

    private final String description;
}
