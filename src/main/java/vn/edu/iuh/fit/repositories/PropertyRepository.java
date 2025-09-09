package vn.edu.iuh.fit.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.iuh.fit.entities.Property;

import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, String> {
    List<Property> findByLandlord_UserId(String landlordId);

    @Query("SELECT DISTINCT p FROM Property p " +
            "LEFT JOIN FETCH p.media " +
            "LEFT JOIN FETCH p.amenities " +
            "LEFT JOIN FETCH p.furnishings " +
            "LEFT JOIN FETCH p.services " +
            "WHERE p.landlord.userId = :landlordId")
    List<Property> findAllByLandlordIdWithDetails(@Param("landlordId") String landlordId);

    @Query("SELECT p FROM Property p")
    List<Property> getAll();

}
