package vn.edu.iuh.fit.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entities.Favorite;

import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, String> {
    List<Favorite> findByTenant_UserId(String tenantId);
    boolean existsByTenant_UserIdAndProperty_PropertyId(String tenantId, String propertyId);
    void deleteByTenant_UserIdAndProperty_PropertyId(String tenantId, String propertyId);
    void deleteAllByTenantUserId(String userId);
}
