package vn.edu.iuh.fit.dtos;

@lombok.Value
public class ContractServiceDto implements java.io.Serializable {
    String id;
    String contractId;
    String serviceName;
    java.math.BigDecimal fee;
    vn.edu.iuh.fit.entities.enums.ChargeBasis chargeBasis;
    Boolean isIncluded;
    String note;
}

