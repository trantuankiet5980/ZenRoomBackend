package vn.edu.iuh.fit.dtos.requests;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import vn.edu.iuh.fit.entities.enums.EventType;

import java.util.Map;

@Getter
@Setter
public class EventRequest {
    @NotNull
    private EventType eventType;
    private String roomId;
    private String query;
    private Map<String, Object> metadata;
}
