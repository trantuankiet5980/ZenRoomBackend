package vn.edu.iuh.fit.mappers;

import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.ContractServiceDto;
import vn.edu.iuh.fit.entities.Contract;
import vn.edu.iuh.fit.entities.ContractService;

@Component
public class ContractServiceMapper {
    public ContractServiceDto toDto(ContractService e) {
        if (e == null) return null;
        return new ContractServiceDto(
                e.getId(),
                e.getContract() != null ? e.getContract().getContractId() : null,
                e.getServiceName(),
                e.getFee(),
                e.getChargeBasis(),
                e.getIsIncluded(),
                e.getNote()
        );
    }

    public ContractService toEntity(ContractServiceDto d) {
        if (d == null) return null;
        return ContractService.builder()
                .id(d.getId())
                .serviceName(d.getServiceName())
                .fee(d.getFee())
                .chargeBasis(d.getChargeBasis())
                .isIncluded(d.getIsIncluded())
                .note(d.getNote())
                .build();
    }

    /** Gắn contract reference sau khi convert */
    public void attachContract(ContractService e, Contract contractRef) {
        if (e != null) e.setContract(contractRef);
    }
}
