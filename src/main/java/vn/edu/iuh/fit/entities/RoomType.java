package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity @Table(name = "RoomTypes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomType {
    @Id
    @Column(name="room_type_id", columnDefinition="CHAR(36)")
    private String roomTypeId;
    @PrePersist
    void pre(){ if(roomTypeId==null) roomTypeId= UUID.randomUUID().toString(); }

    private String typeName;
}
