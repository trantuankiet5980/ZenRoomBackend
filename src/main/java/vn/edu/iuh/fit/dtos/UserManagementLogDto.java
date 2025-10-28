package vn.edu.iuh.fit.dtos;

import lombok.Builder;
import lombok.Value;

import java.io.Serializable;
import java.time.LocalDateTime;

@Value
@Builder
public class UserManagementLogDto implements Serializable {
    String logId;
    String action;
    String adminId;
    String adminName;
    String targetUserId;
    String targetUserName;
    LocalDateTime createdAt;
}
