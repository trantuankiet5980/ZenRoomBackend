package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.services.AdminService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {
    private final AdminService adminService;

    //Duyệt/không duyệt yêu cầu xóa tài khoản
    @PostMapping("{id}/process-deletion")
    public ResponseEntity<Void> processDeletionRequest(
            @PathVariable String id,
            @RequestParam boolean approve,
            @RequestParam(required = false) String reason
    ) {
        adminService.processDeletionRequest(id, approve, reason);
        return ResponseEntity.noContent().build();
    }
}
