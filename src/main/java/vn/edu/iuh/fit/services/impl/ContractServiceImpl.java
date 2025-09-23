package vn.edu.iuh.fit.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.dtos.ContractDto;
import vn.edu.iuh.fit.entities.Booking;
import vn.edu.iuh.fit.entities.Contract;
import vn.edu.iuh.fit.entities.enums.BookingStatus;
import vn.edu.iuh.fit.mappers.ContractMapper;
import vn.edu.iuh.fit.mappers.ContractServiceMapper;
import vn.edu.iuh.fit.repositories.BookingRepository;
import vn.edu.iuh.fit.repositories.ContractRepository;
import vn.edu.iuh.fit.repositories.InvoiceRepository;
import vn.edu.iuh.fit.services.ContractService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepo;
    private final BookingRepository bookingRepo;
    private final InvoiceRepository invoiceRepo;
    private final ContractMapper contractMapper;
    private final ContractServiceMapper serviceMapper;

    private boolean isDepositPaid50(Booking b) {
        // tổng đã thanh toán
        BigDecimal paid = invoiceRepo.findPaidByBooking(b.getBookingId()).stream()
                .map(inv -> inv.getTotal() == null ? BigDecimal.ZERO : inv.getTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal total = b.getTotalPrice() == null ? BigDecimal.ZERO : b.getTotalPrice();
        BigDecimal need = total.multiply(new BigDecimal("0.5"));
        return paid.compareTo(need) >= 0;
    }

    @Transactional
    @Override
    public ContractDto create(String landlordId, ContractDto dto) {
        if (dto == null || dto.getBooking() == null || dto.getBooking().getBookingId() == null)
            throw new IllegalArgumentException("booking (bookingId) is required");

        Booking b = bookingRepo.findById(dto.getBooking().getBookingId()).orElseThrow();

        // quyền: chỉ landlord của booking
        if (b.getProperty() == null || b.getProperty().getLandlord() == null
                || !b.getProperty().getLandlord().getUserId().equals(landlordId)) {
            throw new SecurityException("Only landlord of this booking can create contract");
        }

        // 1 booking 1 contract
        if (contractRepo.findByBooking_BookingId(b.getBookingId()).isPresent()) {
            throw new IllegalStateException("Contract for this booking already exists");
        }

        // trạng thái: đã duyệt & đặt cọc >= 50%
        if (b.getBookingStatus() != BookingStatus.APPROVED && b.getBookingStatus() != BookingStatus.CHECKED_IN) {
            throw new IllegalStateException("Booking must be APPROVED or CHECKED_IN to create contract");
        }
        if (!isDepositPaid50(b)) {
            throw new IllegalStateException("Deposit >= 50% required before creating contract");
        }

        // build entity từ DTO
        Contract entity = contractMapper.toEntity(dto);
        entity.setContractId(entity.getContractId() != null ? entity.getContractId()
                : java.util.UUID.randomUUID().toString());
        entity.setBooking(b);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        // gắn services (cascade ALL)
        if (dto.getServices() != null) {
            List<vn.edu.iuh.fit.entities.ContractService> services = dto.getServices().stream()
                    .map(serviceMapper::toEntity)
                    .peek(s -> serviceMapper.attachContract(s, entity))
                    .toList();
            entity.setServices(services);
        }

        Contract saved = contractRepo.save(entity);
        return contractMapper.toDto(saved);
    }

    @Override
    public ContractDto get(String contractId, String requesterId) {
        Contract c = contractRepo.findById(contractId).orElseThrow();
        Booking b = c.getBooking();
        boolean isTenant = b.getTenant() != null && b.getTenant().getUserId().equals(requesterId);
        boolean isLandlord = b.getProperty() != null && b.getProperty().getLandlord() != null
                && b.getProperty().getLandlord().getUserId().equals(requesterId);
        if (!isTenant && !isLandlord) throw new SecurityException("Not allowed");
        return contractMapper.toDto(c);
    }

    @Override
    public ContractDto getByBooking(String bookingId, String requesterId) {
        Booking b = bookingRepo.findById(bookingId).orElseThrow();
        boolean isTenant = b.getTenant() != null && b.getTenant().getUserId().equals(requesterId);
        boolean isLandlord = b.getProperty() != null && b.getProperty().getLandlord() != null
                && b.getProperty().getLandlord().getUserId().equals(requesterId);
        if (!isTenant && !isLandlord) throw new SecurityException("Not allowed");

        return contractRepo.findByBooking_BookingId(bookingId)
                .map(contractMapper::toDto)
                .orElse(null);
    }

    @Override
    public Page<ContractDto> listMineAsLandlord(String landlordId, Pageable pageable) {
        return contractRepo.findByBooking_Property_Landlord_UserIdOrderByCreatedAtDesc(landlordId, pageable)
                .map(contractMapper::toDto);
    }

    @Override
    public Page<ContractDto> listMineAsTenant(String tenantId, Pageable pageable) {
        return contractRepo.findByBooking_Tenant_UserIdOrderByCreatedAtDesc(tenantId, pageable)
                .map(contractMapper::toDto);
    }

    @Transactional
    @Override
    public ContractDto replace(String landlordId, ContractDto dto) {
        if (dto.getContractId() == null) throw new IllegalArgumentException("contractId is required");
        Contract c = contractRepo.findById(dto.getContractId()).orElseThrow();

        // chỉ landlord được sửa
        if (!c.getBooking().getProperty().getLandlord().getUserId().equals(landlordId))
            throw new SecurityException("Only landlord can update contract");

        // cập nhật các field text/giá trị
        c.setTenantName(dto.getTenantName());
        c.setTenantPhone(dto.getTenantPhone());
        c.setTenantCccdFront(dto.getTenantCccdFront());
        c.setTenantCccdBack(dto.getTenantCccdBack());
        c.setTitle(dto.getTitle());
        c.setRoomNumber(dto.getRoomNumber());
        c.setBuildingName(dto.getBuildingName());
        c.setStartDate(dto.getStartDate());
        c.setEndDate(dto.getEndDate());
        c.setRentPrice(dto.getRentPrice());
        c.setDeposit(dto.getDeposit());
        c.setBillingStartDate(dto.getBillingStartDate());
        c.setPaymentDueDay(dto.getPaymentDueDay());
        c.setNotes(dto.getNotes());
        c.setUpdatedAt(LocalDateTime.now());

        // thay thế danh sách services (orphanRemoval = true giúp xoá cũ)
        if (dto.getServices() != null) {
            c.getServices().clear();
            for (var sdto : dto.getServices()) {
                vn.edu.iuh.fit.entities.ContractService s = serviceMapper.toEntity(sdto);
                serviceMapper.attachContract(s, c);
                c.getServices().add(s);
            }
        }

        return contractMapper.toDto(contractRepo.save(c));
    }
}
