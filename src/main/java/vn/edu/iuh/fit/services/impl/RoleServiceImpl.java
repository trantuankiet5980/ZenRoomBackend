package vn.edu.iuh.fit.services.impl;

import org.springframework.stereotype.Service;
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
    public Role create(Role role) {
        //lower case role name
        if(role.getRoleName() != null){
            role.setRoleName(role.getRoleName().toLowerCase(Locale.ROOT).trim());
        }
        if(roleRepository.existsByRoleName(role.getRoleName())){
            throw new IllegalArgumentException("Role name already exists: " + role.getRoleName());
        }
        if(role.getRoleId() == null) {
            role.setRoleId(UUID.randomUUID().toString());
        }
        return roleRepository.save(role);
    }

    @Override
    public Role getById(String id) {
        return roleRepository.findById(id).orElseThrow(() -> new RuntimeException("Role not found"));
    }

    @Override
    public List<Role> getAll() {
        return roleRepository.findAll();
    }

    @Override
    public Role update(String id, Role role) {
        Role existing = getById(id);
        if (role.getRoleName() != null) {
            String newRoleName = role.getRoleName().toLowerCase(Locale.ROOT).trim();
            if (!newRoleName.equals(existing.getRoleName()) && roleRepository.existsByRoleName(newRoleName)) {
                throw new IllegalArgumentException("Role name already exists: " + newRoleName);
            }
            existing.setRoleName(newRoleName);
        }
        return roleRepository.save(existing);
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
