package vn.edu.iuh.fit.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entities.Province;

public interface ProvinceRepository  extends JpaRepository<Province, String> {
}
