package vn.edu.iuh.fit.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.iuh.fit.entities.PropertyMedia;

import java.util.List;
import java.util.Optional;

public interface PropertyMediaRepository extends JpaRepository<PropertyMedia, String> {
    List<PropertyMedia> findByProperty_PropertyIdOrderBySortOrderAsc(String propertyId);
    Optional<PropertyMedia> findByMediaIdAndProperty_PropertyId(String mediaId, String propertyId);
    @Modifying
    @Query("update PropertyMedia m set m.isCover=false where m.property.propertyId = :propertyId and m.isCover = true")
    void clearCover(@Param("propertyId") String propertyId);
}
