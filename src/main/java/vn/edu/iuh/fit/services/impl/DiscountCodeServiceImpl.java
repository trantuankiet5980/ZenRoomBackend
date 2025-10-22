package vn.edu.iuh.fit.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.dtos.DiscountCodeDto;
import vn.edu.iuh.fit.entities.DiscountCode;
import vn.edu.iuh.fit.entities.enums.DiscountCodeStatus;
import vn.edu.iuh.fit.entities.enums.DiscountType;
import vn.edu.iuh.fit.mappers.DiscountCodeMapper;
import vn.edu.iuh.fit.repositories.DiscountCodeRepository;
import vn.edu.iuh.fit.services.DiscountCodeService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiscountCodeServiceImpl implements DiscountCodeService {

    private final DiscountCodeRepository repo;
    private final DiscountCodeMapper mapper;

    @Transactional
    @Override
    public DiscountCodeDto create(String adminId, DiscountCodeDto dto) {
        validate(dto, true);

        DiscountCode e = mapper.toEntity(dto);
        e.setCodeId(UUID.randomUUID().toString());
        e.setCode(dto.getCode().trim().toUpperCase(Locale.ROOT));
        if (e.getUsedCount() == null) e.setUsedCount(0);
        if (e.getStatus() == null) e.setStatus(DiscountCodeStatus.ACTIVE);

        e.setStatus(effectiveStatus(e));
        return mapper.toDto(repo.save(e));
    }

    @Transactional
    @Override
    public DiscountCodeDto update(String adminId, DiscountCodeDto dto) {
        if (dto.getCodeId() == null) throw new IllegalArgumentException("codeId is required");
        validate(dto, false);

        DiscountCode e = repo.findById(dto.getCodeId()).orElseThrow();
        // Không cho đổi code sang code trùng
        String newCode = dto.getCode().trim().toUpperCase(Locale.ROOT);
        if (!newCode.equalsIgnoreCase(e.getCode()) && repo.existsByCodeIgnoreCase(newCode)) {
            throw new IllegalArgumentException("code already exists");
        }

        e.setCode(newCode);
        e.setDescription(dto.getDescription());
        e.setDiscountType(dto.getDiscountType());
        e.setDiscountValue(dto.getDiscountValue());
        e.setValidFrom(dto.getValidFrom());
        e.setValidTo(dto.getValidTo());
        e.setUsageLimit(dto.getUsageLimit());
        // usedCount giữ nguyên nếu dto.getUsedCount() null
        if (dto.getUsedCount() != null) e.setUsedCount(dto.getUsedCount());
        e.setStatus(dto.getStatus() != null ? dto.getStatus() : e.getStatus());

        e.setStatus(effectiveStatus(e));
        return mapper.toDto(repo.save(e));
    }

    @Transactional
    @Override
    public void delete(String adminId, String codeId) {
        repo.deleteById(codeId);
    }

    @Override
    public DiscountCodeDto get(String codeId) {
        return repo.findById(codeId).map(c -> {
            c.setStatus(effectiveStatus(c));
            return mapper.toDto(c);
        }).orElse(null);
    }

    @Override
    public Page<DiscountCodeDto> list(String q,
                                      List<DiscountCodeStatus> statuses,
                                      LocalDate validFrom,
                                      LocalDate validTo,
                                      Pageable pageable) {
        String keyword = (q == null || q.trim().isEmpty()) ? null : q.trim();
        List<DiscountCodeStatus> statusFilters = statuses == null ? List.of() :
                statuses.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        boolean statusesEmpty = statusFilters.isEmpty();

        return repo.search(keyword, statusFilters, statusesEmpty, validFrom, validTo, pageable).map(c -> {
            c.setStatus(effectiveStatus(c));
            return mapper.toDto(c);
        });
    }

    @Override
    public BigDecimal previewDiscount(String code, BigDecimal subtotal) {
        DiscountCode e = repo.findByCodeIgnoreCase(code.trim()).orElseThrow();
        if (effectiveStatus(e) != DiscountCodeStatus.ACTIVE) throw new IllegalStateException("Code not active");
        if (!isWithinDate(e)) throw new IllegalStateException("Code out of date");
        if (e.getUsageLimit() != null && e.getUsedCount() != null && e.getUsedCount() >= e.getUsageLimit())
            throw new IllegalStateException("Code usage limit reached");
        return calcDiscount(e, subtotal);
    }

    @Transactional
    @Override
    public BigDecimal applyDiscount(String code, BigDecimal subtotal) {
        DiscountCode e = repo.findByCodeIgnoreCase(code.trim()).orElseThrow();
        if (effectiveStatus(e) != DiscountCodeStatus.ACTIVE) throw new IllegalStateException("Code not active");
        if (!isWithinDate(e)) throw new IllegalStateException("Code out of date");

        // Tăng usedCount an toàn (atomic), tránh race condition
        int ok = repo.tryConsumeOnce(e.getCodeId());
        if (ok == 0) throw new IllegalStateException("Code usage limit reached");

        // reload usedCount mới (tuỳ chọn)
        e = repo.findById(e.getCodeId()).orElseThrow();
        return calcDiscount(e, subtotal);
    }

    private void validate(DiscountCodeDto d, boolean isCreate) {
        if (d.getCode() == null || d.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("code is required");
        }
        String normalized = d.getCode().trim().toUpperCase(Locale.ROOT);
        if (isCreate && repo.existsByCodeIgnoreCase(normalized)) {
            throw new IllegalArgumentException("code already exists");
        }
        if (d.getDiscountType() == null) throw new IllegalArgumentException("discountType is required");
        if (d.getDiscountValue() == null || d.getDiscountValue().compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("discountValue must be >= 0");
        if (d.getDiscountType() == DiscountType.PERCENT &&
                (d.getDiscountValue().compareTo(BigDecimal.ZERO) < 0 ||
                        d.getDiscountValue().compareTo(new BigDecimal("100")) > 0)) {
            throw new IllegalArgumentException("percent must be between 0 and 100");
        }
        if (d.getValidFrom() != null && d.getValidTo() != null
                && d.getValidTo().isBefore(d.getValidFrom())) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
    }

    private boolean isWithinDate(DiscountCode e) {
        LocalDate today = LocalDate.now();
        if (e.getValidFrom() != null && today.isBefore(e.getValidFrom())) return false;
        if (e.getValidTo() != null && today.isAfter(e.getValidTo())) return false;
        return true;
    }

    private DiscountCodeStatus effectiveStatus(DiscountCode e) {
        if (e.getStatus() == DiscountCodeStatus.EXPIRED) return DiscountCodeStatus.EXPIRED;
        return isWithinDate(e) ? DiscountCodeStatus.ACTIVE : DiscountCodeStatus.EXPIRED;
    }

    private BigDecimal calcDiscount(DiscountCode e, BigDecimal subtotal) {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        BigDecimal off;
        if (e.getDiscountType() == DiscountType.PERCENT) {
            off = subtotal.multiply(e.getDiscountValue()).divide(new BigDecimal("100"));
        } else {
            off = e.getDiscountValue();
        }
        if (off.compareTo(subtotal) > 0) off = subtotal; // không âm
        return off;
    }
}
