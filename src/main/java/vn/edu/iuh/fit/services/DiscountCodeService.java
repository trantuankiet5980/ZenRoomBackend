package vn.edu.iuh.fit.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.edu.iuh.fit.dtos.DiscountCodeDto;
import vn.edu.iuh.fit.entities.enums.DiscountCodeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface DiscountCodeService {
    DiscountCodeDto create(String adminId, DiscountCodeDto dto);
    DiscountCodeDto update(String adminId, DiscountCodeDto dto);
    void delete(String adminId, String codeId); // tuỳ chọn

    DiscountCodeDto get(String codeId);
    Page<DiscountCodeDto> list(String q,
                               List<DiscountCodeStatus> statuses,
                               LocalDate validFrom,
                               LocalDate validTo,
                               Pageable pageable);

    /** Kiểm tra & áp mã lên subtotal, trả về số tiền được giảm */
    BigDecimal previewDiscount(String code, BigDecimal subtotal);

    /** Áp mã thật sự (tăng usedCount an toàn). Trả về số tiền giảm được, hoặc throw nếu không hợp lệ. */
    BigDecimal applyDiscount(String code, BigDecimal subtotal);
}
