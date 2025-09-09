package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.iuh.fit.dtos.PropertyMediaDto;
import vn.edu.iuh.fit.entities.PropertyMedia;
import vn.edu.iuh.fit.mappers.PropertyMediaMapper;
import vn.edu.iuh.fit.services.PropertyMediaService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/properties/{propertyId}/media")
public class PropertyMediaController {

    private final PropertyMediaService mediaService;
    private final PropertyMediaMapper mediaMapper;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<PropertyMediaDto>> upload(
            @PathVariable String propertyId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(defaultValue = "IMAGE") vn.edu.iuh.fit.entities.enums.MediaType mediaType,
            @RequestParam(required = false) Integer startOrder,
            @RequestParam(defaultValue = "false") boolean firstAsCover
    ) throws IOException {

        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        int order = (startOrder != null) ? startOrder : 0;
        List<PropertyMediaDto> result = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile f = files.get(i);
            if (f == null || f.isEmpty()) continue;

            boolean cover = firstAsCover && i == 0;

            // (optional) validate content-type theo mediaType
            String ct = f.getContentType();
            if (mediaType == vn.edu.iuh.fit.entities.enums.MediaType.IMAGE && (ct == null || !ct.startsWith("image/"))) {
                throw new IllegalArgumentException("File #" + i + " must be an image");
            }
            if (mediaType == vn.edu.iuh.fit.entities.enums.MediaType.VIDEO && (ct == null || !ct.startsWith("video/"))) {
                throw new IllegalArgumentException("File #" + i + " must be a video");
            }

            PropertyMedia saved = mediaService.upload(propertyId, f, mediaType, order++, cover);
            result.add(mediaMapper.toDto(saved));
        }

        return ResponseEntity.created(null).body(result); // 201
    }

    @GetMapping
    public ResponseEntity<List<PropertyMediaDto>> list(
            @PathVariable String propertyId,
            @RequestParam(defaultValue = "false") boolean presign
    ) {
        return ResponseEntity.ok(mediaService.list(propertyId, presign, mediaMapper));
    }

    @PostMapping("/{mediaId}/cover")
    public ResponseEntity<Void> setCover(@PathVariable String propertyId, @PathVariable String mediaId) {
        mediaService.setCover(propertyId, mediaId);
        return ResponseEntity.noContent().build(); // 204
    }

    @DeleteMapping("/{mediaId}")
    public ResponseEntity<Void> delete(@PathVariable String propertyId, @PathVariable String mediaId) {
        mediaService.delete(mediaId);
        return ResponseEntity.noContent().build(); // 204
    }
}

