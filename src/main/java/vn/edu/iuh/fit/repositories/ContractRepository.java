package vn.edu.iuh.fit.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entities.Contract;

import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, String> {
    Optional<Contract> findByBooking_BookingId(String bookingId);

    Page<Contract> findByBooking_Property_Landlord_UserIdOrderByCreatedAtDesc(String landlordId, Pageable pageable);

    Page<Contract> findByBooking_Tenant_UserIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);
}
