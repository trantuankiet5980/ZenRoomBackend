package vn.edu.iuh.fit.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entities.Booking;

public interface BookingRepository extends JpaRepository<Booking, String> {
}
