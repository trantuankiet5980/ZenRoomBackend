package vn.edu.iuh.fit.services;

import vn.edu.iuh.fit.dtos.requests.EventRequest;

public interface EventService {
    void recordEvent(String userId, EventRequest request);
}
