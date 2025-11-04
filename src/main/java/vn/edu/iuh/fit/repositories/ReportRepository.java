package vn.edu.iuh.fit.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entities.Report;

public interface ReportRepository extends JpaRepository<Report, String> {
}
