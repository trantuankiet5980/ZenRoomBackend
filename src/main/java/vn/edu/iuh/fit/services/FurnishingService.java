package vn.edu.iuh.fit.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import vn.edu.iuh.fit.dtos.FurnishingWithQuantityDto;
import vn.edu.iuh.fit.dtos.FurnishingsDto;
import vn.edu.iuh.fit.entities.Furnishings;

import java.util.List;
import java.util.Optional;

public interface FurnishingService {
    Page<FurnishingsDto> list(String q, Pageable pageable);
    Optional<FurnishingsDto> get(String id);
    FurnishingsDto create(FurnishingsDto dto);
    FurnishingsDto update(String id, FurnishingsDto dto);
    void delete(String id);

    List<FurnishingWithQuantityDto> getFurnishingsOfProperty(String propertyId);
}
