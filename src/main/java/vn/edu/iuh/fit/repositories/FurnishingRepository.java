package vn.edu.iuh.fit.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entities.Furnishings;

public interface FurnishingRepository extends JpaRepository<Furnishings, String> {
    // Search theo tên
    Page<Furnishings> findByFurnishingNameContainingIgnoreCase(String name, Pageable pageable);

    boolean existsByFurnishingNameIgnoreCase(String name);
}
