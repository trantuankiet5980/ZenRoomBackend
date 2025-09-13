package vn.edu.iuh.fit.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.iuh.fit.dtos.FurnishingWithQuantityDto;
import vn.edu.iuh.fit.dtos.FurnishingsDto;
import vn.edu.iuh.fit.entities.PropertyFurnishing;

import java.util.List;

public interface PropertyFurnishingRepository extends JpaRepository<PropertyFurnishing, String> {
    @Query("""
        SELECT new vn.edu.iuh.fit.dtos.FurnishingWithQuantityDto(
            f.furnishingId, f.furnishingName, pf.quantity
        )
        FROM PropertyFurnishing pf
        JOIN pf.furnishing f
        WHERE pf.property.propertyId = :propertyId
        ORDER BY f.furnishingName ASC
    """)
    List<FurnishingWithQuantityDto> findFurnishingsByPropertyId(@Param("propertyId") String propertyId);
}
