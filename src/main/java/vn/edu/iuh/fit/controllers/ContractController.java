package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.ContractDto;
import vn.edu.iuh.fit.services.ContractService;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ContractController {
    private final ContractService contractService;

    /** Landlord tạo hợp đồng (kèm services) */
    @PostMapping
    public ContractDto create(@RequestBody ContractDto dto, Principal principal) {
        return contractService.create(principal.getName(), dto);
    }

    /** Xem chi tiết hợp đồng (tenant/landlord của booking mới xem) */
    @GetMapping("/{contractId}")
    public ContractDto get(@PathVariable String contractId, Principal principal) {
        return contractService.get(contractId, principal.getName());
    }

    /** Xem hợp đồng theo booking (tenant/landlord của booking mới xem) */
    @GetMapping("/by-booking/{bookingId}")
    public ContractDto getByBooking(@PathVariable String bookingId, Principal principal) {
        return contractService.getByBooking(bookingId, principal.getName());
    }

    /** Danh sách hợp đồng của tôi (Landlord) */
    @GetMapping("/me/landlord")
    public Page<ContractDto> myContractsAsLandlord(Principal principal,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @RequestParam(defaultValue="createdAt,DESC") String sort) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        return contractService.listMineAsLandlord(principal.getName(), pageable);
    }

    /** Danh sách hợp đồng của tôi (Tenant) */
    @GetMapping("/me/tenant")
    public Page<ContractDto> myContractsAsTenant(Principal principal,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(defaultValue="createdAt,DESC") String sort) {
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        return contractService.listMineAsTenant(principal.getName(), pageable);
    }

    /** Landlord cập nhật hợp đồng (replace toàn bộ, gồm services) */
    @PutMapping
    public ContractDto replace(@RequestBody ContractDto dto, Principal principal) {
        return contractService.replace(principal.getName(), dto);
    }

    private Sort parseSort(String sort) {
        String[] p = sort.split(",");
        if (p.length == 2) return Sort.by("DESC".equalsIgnoreCase(p[1]) ? Sort.Direction.DESC : Sort.Direction.ASC, p[0]);
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }
}
