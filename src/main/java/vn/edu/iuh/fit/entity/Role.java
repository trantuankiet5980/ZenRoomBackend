package vn.edu.iuh.fit.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;


@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "Roles")
public class Role {
    @Id
    @Column(name = "role_id", columnDefinition = "CHAR(36)")
    private String roleId;

    @PrePersist
    void prePersist() {
        if (this.roleId == null) this.roleId = UUID.randomUUID().toString();
    }

    @Column(name = "role_name", unique = true, nullable = false, length = 50)
    private String roleName;

    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    @Builder.Default private List<User> users = new ArrayList<>();
}
