package vn.edu.iuh.fit.repositories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.edu.iuh.fit.entities.Property;

import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, String>, JpaSpecificationExecutor<Property> {

    @EntityGraph(attributePaths = {
            "landlord",
            "address",
            "furnishings",
            "furnishings.furnishing",
            "media"
    })
    Optional<Property> findWithDetailsByPropertyId(String propertyId);
}
