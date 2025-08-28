package vn.edu.iuh.fit.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.entities.Role;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {
    //ROLE: tenant | landlord | admin
    Optional<Role> findByRoleName(String roleName);
    boolean existsByRoleName(String roleName);
}
