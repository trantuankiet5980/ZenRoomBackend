package vn.edu.iuh.fit.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;


@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "RoomImages")
public class RoomImage {
    @Id
    @Column(name = "image_id", columnDefinition = "CHAR(36)")
    private String imageId;

    @PrePersist
    void prePersist() {
        if (this.imageId == null) this.imageId = UUID.randomUUID().toString();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", referencedColumnName = "room_id")
    private Room room;

    @Column(name = "image_url", length = 255)
    private String imageUrl;
}
