package vn.edu.iuh.fit.services;

import org.springframework.web.multipart.MultipartFile;
import vn.edu.iuh.fit.dtos.PropertyMediaDto;
import vn.edu.iuh.fit.entities.PropertyMedia;
import vn.edu.iuh.fit.entities.enums.MediaType;
import vn.edu.iuh.fit.mappers.PropertyMediaMapper;

import java.io.IOException;
import java.util.List;

public interface PropertyMediaService {
    PropertyMedia upload(String propertyId, MultipartFile file, MediaType mediaType, Integer sortOrder, Boolean isCover) throws IOException;
    List<PropertyMediaDto> list(String propertyId, boolean presign, PropertyMediaMapper mapper);

    void setCover(String propertyId, String mediaId);
    void delete(String mediaId);
}
