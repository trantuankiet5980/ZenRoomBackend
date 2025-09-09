package vn.edu.iuh.fit.mappers;

import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.ContractDto;
import vn.edu.iuh.fit.entities.Contract;

@Component
public class ContractMapper {

    public ContractDto toDto(Contract entity) {
        if (entity == null) return null;
        return new ContractDto(
                entity.getContractId(),
                entity.getTenantName(),
                entity.getTenantPhone(),
                entity.getTenantCccdFront(),
                entity.getTenantCccdBack(),
                entity.getTitle(),
                entity.getRoomNumber(),
                entity.getBuildingName(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getRentPrice(),
                entity.getDeposit(),
                entity.getBillingStartDate(),
                entity.getPaymentDueDay(),
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public Contract toEntity(ContractDto dto) {
        if (dto == null) return null;
        Contract entity = new Contract();
        entity.setContractId(dto.getContractId());
        entity.setTenantName(dto.getTenantName());
        entity.setTenantPhone(dto.getTenantPhone());
        entity.setTenantCccdFront(dto.getTenantCccdFront());
        entity.setTenantCccdBack(dto.getTenantCccdBack());
        entity.setTitle(dto.getTitle());
        entity.setRoomNumber(dto.getRoomNumber());
        entity.setBuildingName(dto.getBuildingName());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setRentPrice(dto.getRentPrice());
        entity.setDeposit(dto.getDeposit());
        entity.setBillingStartDate(dto.getBillingStartDate());
        entity.setPaymentDueDay(dto.getPaymentDueDay());
        entity.setNotes(dto.getNotes());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        return entity;
    }
}
