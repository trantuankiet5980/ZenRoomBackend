package vn.edu.iuh.fit.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.edu.iuh.fit.dtos.ContractDto;

public interface ContractService {
    ContractDto create(String landlordId, ContractDto dto);
    ContractDto get(String contractId, String requesterId);
    ContractDto getByBooking(String bookingId, String requesterId);

    Page<ContractDto> listMineAsLandlord(String landlordId, Pageable pageable);
    Page<ContractDto> listMineAsTenant(String tenantId, Pageable pageable);

    // tuỳ chọn: cập nhật hợp đồng (thay lỗi chính tả, ghi chú, thay services…)
    ContractDto replace(String landlordId, ContractDto dto);

    //Export contract to PDF
    byte[] exportPdf(String contractId, String requesterId);
}
