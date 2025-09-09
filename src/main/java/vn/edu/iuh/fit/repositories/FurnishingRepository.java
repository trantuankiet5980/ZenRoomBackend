package vn.edu.iuh.fit.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entities.Furnishings;

public interface FurnishingRepository extends JpaRepository<Furnishings, String> {
}
