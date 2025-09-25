package vn.edu.iuh.fit.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entities.District;

import java.util.List;

public interface DistrictRepository extends JpaRepository<District, String> {
    List<District> findByProvince_Code(String provinceCode);
}