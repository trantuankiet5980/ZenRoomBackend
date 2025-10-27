package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.iuh.fit.dtos.UserManagementLogDto;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.entities.UserManagementLog;
import vn.edu.iuh.fit.repositories.UserManagementLogRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/user-management-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementLogController {

    private final UserManagementLogRepository userManagementLogRepository;

    @GetMapping
    public List<UserManagementLogDto> getLogs(
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {

        List<UserManagementLog> logs;

        if (fromDate == null && toDate == null) {
            logs = userManagementLogRepository.findAllByOrderByCreatedAtDesc();
        } else if (fromDate != null && toDate == null) {
            LocalDateTime from = fromDate.atStartOfDay();
            LocalDateTime to = LocalDateTime.now();
            logs = userManagementLogRepository
                    .findAllByCreatedAtBetweenOrderByCreatedAtDesc(from, to);
        } else if (fromDate == null && toDate != null) {
            LocalDateTime from = LocalDate.of(1970, 1, 1).atStartOfDay();
            LocalDateTime to = toDate.atTime(LocalTime.MAX);
            logs = userManagementLogRepository
                    .findAllByCreatedAtBetweenOrderByCreatedAtDesc(from, to);
        } else {
            if (fromDate.isAfter(toDate)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "fromDate must be before or equal to toDate");
            }

            LocalDateTime from = fromDate.atStartOfDay();
            LocalDateTime to = toDate.atTime(LocalTime.MAX);

            logs = userManagementLogRepository
                    .findAllByCreatedAtBetweenOrderByCreatedAtDesc(from, to);
        }

        return logs.stream()
                .map(UserManagementLogController::toDto)
                .collect(Collectors.toList());
    }

    private static UserManagementLogDto toDto(UserManagementLog log) {
        User admin = log.getAdmin();
        User target = log.getTargetUser();

        return UserManagementLogDto.builder()
                .logId(log.getLogId())
                .action(log.getAction())
                .createdAt(log.getCreatedAt())
                .adminId(admin != null ? admin.getUserId() : null)
                .adminName(admin != null ? admin.getFullName() : null)
                .targetUserId(target != null ? target.getUserId() : null)
                .targetUserName(target != null ? target.getFullName() : null)
                .build();
    }
}
