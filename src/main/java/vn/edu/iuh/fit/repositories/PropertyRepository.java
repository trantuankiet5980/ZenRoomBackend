package vn.edu.iuh.fit.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entities.Property;

public interface PropertyRepository extends JpaRepository<Property, String> {
}
