package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.dtos.RoomTypeDto;
import vn.edu.iuh.fit.services.RoomTypeService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/room-types")
public class RoomTypeController {

    private final RoomTypeService roomTypeService;

    @GetMapping
    public ResponseEntity<List<RoomTypeDto>> getAll() {
        return ResponseEntity.ok(roomTypeService.getAllRoomTypes());
    }
}
