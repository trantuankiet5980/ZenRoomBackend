package vn.edu.iuh.fit.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entities.Amenity;

public interface AmenityRepository extends JpaRepository<Amenity, String> {
}
