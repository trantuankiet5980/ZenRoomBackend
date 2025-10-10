package vn.edu.iuh.fit.services;

import vn.edu.iuh.fit.dtos.PropertyDto;
import vn.edu.iuh.fit.dtos.requests.EventRequest;

import java.util.List;

public interface EventService {
    void recordEvent(String userId, EventRequest request);

    List<PropertyDto> getRecentlyViewedProperties(String userId, int limit);
}
