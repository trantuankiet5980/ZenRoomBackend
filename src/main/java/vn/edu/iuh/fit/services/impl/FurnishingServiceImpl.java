package vn.edu.iuh.fit.services.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.dtos.FurnishingWithQuantityDto;
import vn.edu.iuh.fit.dtos.FurnishingsDto;
import vn.edu.iuh.fit.entities.Furnishings;
import vn.edu.iuh.fit.mappers.FurnishingsMapper;
import vn.edu.iuh.fit.repositories.FurnishingRepository;
import vn.edu.iuh.fit.repositories.PropertyFurnishingRepository;
import vn.edu.iuh.fit.services.FurnishingService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FurnishingServiceImpl implements FurnishingService {

    private final FurnishingRepository repo;
    private final FurnishingsMapper mapper;
    private final PropertyFurnishingRepository propertyFurnishingRepository;

    @Transactional(readOnly = true)
    @Override
    public Page<FurnishingsDto> list(String q, Pageable pageable) {
        Page<Furnishings> page = (q == null || q.isBlank())
                ? repo.findAll(pageable)
                : repo.findByFurnishingNameContainingIgnoreCase(q.trim(), pageable);
        return page.map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<FurnishingsDto> get(String id) {
        return repo.findById(id).map(mapper::toDto);
    }

    @Transactional
    @Override
    public FurnishingsDto create(FurnishingsDto dto) {
        if (dto == null || dto.getFurnishingName() == null || dto.getFurnishingName().isBlank()) {
            throw new IllegalArgumentException("furnishingName is required");
        }
        if (repo.existsByFurnishingNameIgnoreCase(dto.getFurnishingName().trim())) {
            throw new IllegalArgumentException("furnishingName already exists");
        }
        Furnishings entity = new Furnishings();
        entity.setFurnishingName(dto.getFurnishingName().trim());
        Furnishings saved = repo.save(entity);
        return mapper.toDto(saved);
    }

    @Transactional
    @Override
    public FurnishingsDto update(String id, FurnishingsDto dto) {
        Furnishings entity = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Furnishing not found: " + id));

        if (dto.getFurnishingName() != null && !dto.getFurnishingName().isBlank()) {
            String newName = dto.getFurnishingName().trim();
            if (!newName.equalsIgnoreCase(entity.getFurnishingName())
                    && repo.existsByFurnishingNameIgnoreCase(newName)) {
                throw new IllegalArgumentException("furnishingName already exists");
            }
            entity.setFurnishingName(newName);
        }

        Furnishings saved = repo.save(entity);
        return mapper.toDto(saved);
    }

    @Transactional
    @Override
    public void delete(String id) {
        Furnishings entity = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Furnishing not found: " + id));

        repo.delete(entity);
    }

    @Override
    public List<FurnishingWithQuantityDto> getFurnishingsOfProperty(String propertyId) {
        return propertyFurnishingRepository.findFurnishingsByPropertyId(propertyId);
    }
}
