package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "user_follows",
        uniqueConstraints = @UniqueConstraint(name="uk_follower_following", columnNames={"follower_id","following_id"}),
        indexes = {
                @Index(name="idx_follows_follower", columnList="follower_id"),
                @Index(name="idx_follows_following", columnList="following_id")
        })
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserFollow {
    @Id @Column(name="id", columnDefinition="CHAR(36)")
    private String id;

    @PrePersist void pre() { if (id==null) id= UUID.randomUUID().toString(); if (createdAt==null) createdAt=LocalDateTime.now(); }

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="follower_id", nullable=false)
    private User follower;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="following_id", nullable=false)
    private User following;

    private LocalDateTime createdAt;
}
