package vn.edu.iuh.fit.services.impl;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.dtos.role.RoleCreateRequest;
import vn.edu.iuh.fit.dtos.role.RoleResponse;
import vn.edu.iuh.fit.dtos.role.RoleUpdateRequest;
import vn.edu.iuh.fit.entities.Role;
import vn.edu.iuh.fit.repositories.RoleRepository;
import vn.edu.iuh.fit.services.RoleService;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }
    @Override
    public RoleResponse create(RoleCreateRequest req) {
        if (req == null || req.roleName() == null || req.roleName().isBlank())
            throw new IllegalArgumentException("roleName is required");

        String name = req.roleName().toLowerCase(Locale.ROOT).trim();
        if (roleRepository.existsByRoleName(name)) throw new IllegalArgumentException("Role already exists: " + name);

        Role r = new Role();
        r.setRoleId(UUID.randomUUID().toString());
        r.setRoleName(name);
        roleRepository.save(r);

        return new RoleResponse(r.getRoleId(), r.getRoleName());
    }

    @Override
    public RoleResponse  getById(String id) {
        Role r = roleRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Role not found"));
        return new RoleResponse(r.getRoleId(), r.getRoleName());    }

    @Override
    public Page<RoleResponse> list(Pageable pageable) {
        return roleRepository.findAll(pageable).map(r -> new RoleResponse(r.getRoleId(), r.getRoleName()));
    }

    @Override
    public RoleResponse update(String id, RoleUpdateRequest req) {
        Role r = roleRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Role not found"));
        if (req.roleName() != null && !req.roleName().isBlank()) {
            String name = req.roleName().toLowerCase(Locale.ROOT).trim();
            if (!name.equals(r.getRoleName()) && roleRepository.existsByRoleName(name))
                throw new IllegalArgumentException("Role already exists: " + name);
            r.setRoleName(name);
        }
        roleRepository.save(r);
        return new RoleResponse(r.getRoleId(), r.getRoleName());
    }

    @Override
    public void delete(String id) {
        try{
            roleRepository.deleteById(id);
        } catch (Exception ex){
            throw new IllegalStateException("Cannot delete role: it is referenced by users.", ex);
        }
    }
}
