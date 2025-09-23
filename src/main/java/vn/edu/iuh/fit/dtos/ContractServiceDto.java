package vn.edu.iuh.fit.dtos;

import vn.edu.iuh.fit.entities.enums.ChargeBasis;

@lombok.Value
public class ContractServiceDto implements java.io.Serializable {
    String id;
    String contractId;
    String serviceName;
    java.math.BigDecimal fee;
    ChargeBasis chargeBasis;
    Boolean isIncluded;
    String note;
}

