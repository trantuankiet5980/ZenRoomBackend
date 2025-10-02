package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Province {
    @Id
    private String code;
    private String name_with_type;

    @OneToMany(mappedBy = "province", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<District> districts;

    public Province(String code, String name_with_type) {
        this.code = code;
        this.name_with_type = name_with_type;
    }
}
