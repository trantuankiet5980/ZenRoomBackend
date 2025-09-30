package vn.edu.iuh.fit.mappers;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.ContractDto;
import vn.edu.iuh.fit.dtos.ContractServiceDto;
import vn.edu.iuh.fit.entities.Contract;
import vn.edu.iuh.fit.entities.ContractService;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ContractMapper {
    private final BookingMapper bookingMapper;
    private final ContractServiceMapper serviceMapper;

    /** Entity -> DTO */
    public ContractDto toDto(Contract e) {
        if (e == null) return null;
        List<ContractServiceDto> services = e.getServices() != null
                ? e.getServices().stream().map(serviceMapper::toDto).collect(Collectors.toList())
                : List.of();

        return new ContractDto(
                e.getContractId(),
                bookingMapper.toDto(e.getBooking()),
                e.getTenantName(),
                e.getTenantPhone(),
                e.getTenantCccdFront(),
                e.getTenantCccdBack(),
                e.getTitle(),
                e.getRoomNumber(),
                e.getBuildingName(),
                e.getStartDate(),
                e.getEndDate(),
                e.getContractStatus(),
                services,
                e.getRentPrice(),
                e.getDeposit(),
                e.getBillingStartDate(),
                e.getPaymentDueDay(),
                e.getNotes(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    /** DTO -> Entity (booking gán ở service để tránh vòng lặp) */
    public Contract toEntity(ContractDto d) {
        if (d == null) return null;
        Contract c = new Contract();
        c.setContractId(d.getContractId());
        c.setTenantName(d.getTenantName());
        c.setTenantPhone(d.getTenantPhone());
        c.setTenantCccdFront(d.getTenantCccdFront());
        c.setTenantCccdBack(d.getTenantCccdBack());
        c.setTitle(d.getTitle());
        c.setRoomNumber(d.getRoomNumber());
        c.setBuildingName(d.getBuildingName());
        c.setStartDate(d.getStartDate());
        c.setEndDate(d.getEndDate());
        c.setContractStatus(d.getContractStatus());
        c.setRentPrice(d.getRentPrice());
        c.setDeposit(d.getDeposit());
        c.setBillingStartDate(d.getBillingStartDate());
        c.setPaymentDueDay(d.getPaymentDueDay());
        c.setNotes(d.getNotes());
        c.setCreatedAt(d.getCreatedAt());
        c.setUpdatedAt(d.getUpdatedAt());

        if (d.getServices() != null) {
            List<ContractService> services = d.getServices().stream()
                    .map(serviceMapper::toEntity)
                    .peek(s -> s.setContract(c)) // gán ngược quan hệ
                    .collect(Collectors.toList());
            c.setServices(services);
        }
        return c;
    }
}
