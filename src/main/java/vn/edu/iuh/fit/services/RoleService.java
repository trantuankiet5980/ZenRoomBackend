package vn.edu.iuh.fit.services;

import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entities.Role;

import java.util.List;

@Service
public interface RoleService {
    Role create(Role role);
    Role getById(String id);
    List<Role> getAll();
    Role update(String id, Role role);
    void delete(String id);
}
