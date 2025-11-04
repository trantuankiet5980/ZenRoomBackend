package vn.edu.iuh.fit.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.edu.iuh.fit.entities.Report;

public interface ReportRepository extends JpaRepository<Report, String>, JpaSpecificationExecutor<Report> {
}
