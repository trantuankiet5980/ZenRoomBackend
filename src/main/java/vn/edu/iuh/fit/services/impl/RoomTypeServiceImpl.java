package vn.edu.iuh.fit.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.dtos.RoomTypeDto;
import vn.edu.iuh.fit.repositories.RoomTypeRepository;
import vn.edu.iuh.fit.services.RoomTypeService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomTypeServiceImpl implements RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;

    @Override
    public List<RoomTypeDto> getAllRoomTypes() {
        return roomTypeRepository.findAll()
                .stream()
                .map(roomType -> new RoomTypeDto(
                        roomType.getRoomTypeId(),
                        roomType.getTypeName()
                ))
                .toList();
    }
}
