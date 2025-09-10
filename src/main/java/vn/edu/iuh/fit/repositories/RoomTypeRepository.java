package vn.edu.iuh.fit.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entities.RoomType;

import java.util.Optional;

public interface RoomTypeRepository extends JpaRepository<RoomType, String> {
}
