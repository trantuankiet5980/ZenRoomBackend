package vn.edu.iuh.fit.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entities.Address;

public interface AddressRepository extends JpaRepository<Address, String> {
}
