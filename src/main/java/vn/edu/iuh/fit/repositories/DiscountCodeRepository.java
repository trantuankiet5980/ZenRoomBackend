package vn.edu.iuh.fit.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import vn.edu.iuh.fit.entities.DiscountCode;
import vn.edu.iuh.fit.entities.enums.DiscountCodeStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DiscountCodeRepository extends JpaRepository<DiscountCode, String> {
    Optional<DiscountCode> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    @Query("""
      select d from DiscountCode d
      where (:q is null or lower(d.code) like lower(concat('%', :q, '%'))
         or  lower(d.description) like lower(concat('%', :q, '%')))
         and (:statusesEmpty = true or d.status in :statuses)
                 and (:validFrom is null or d.validFrom is null or d.validFrom >= :validFrom)
                 and (:validTo is null or d.validTo is null or d.validTo <= :validTo)
    """)
    Page<DiscountCode> search(String q,
                              List<DiscountCodeStatus> statuses,
                              boolean statusesEmpty,
                              LocalDate validFrom,
                              LocalDate validTo,
                              Pageable pageable);

    @Modifying
    @Query("""
      update DiscountCode d
         set d.usedCount = d.usedCount + 1
       where d.codeId = :codeId
         and (d.usageLimit is null or d.usedCount < d.usageLimit)
    """)
    int tryConsumeOnce(String codeId); // 1 = OK, 0 = hết lượt
}
