package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class District {
    @Id
    private String code; // VD: 760
    private String name_with_type; // Quận 1

    @ManyToOne
    @JoinColumn(name = "province_code")
    private Province province;

    @OneToMany(mappedBy = "district", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Ward> wards;

    public District(String code, String name_with_type, Province province) {
        this.code = code;
        this.name_with_type = name_with_type;
        this.province = province;
    }
}
