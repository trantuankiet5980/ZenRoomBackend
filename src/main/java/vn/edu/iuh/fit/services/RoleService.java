package vn.edu.iuh.fit.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.dtos.role.RoleCreateRequest;
import vn.edu.iuh.fit.dtos.role.RoleResponse;
import vn.edu.iuh.fit.dtos.role.RoleUpdateRequest;

@Service
public interface RoleService {
    RoleResponse create(RoleCreateRequest req);
    RoleResponse getById(String id);
    Page<RoleResponse> list(Pageable pageable);
    RoleResponse update(String id, RoleUpdateRequest req);
    void delete(String id);
}
