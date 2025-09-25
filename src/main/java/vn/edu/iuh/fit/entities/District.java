package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class District {
    @Id
    private String code; // VD: 760
    private String name; // Quận 1

    @ManyToOne
    @JoinColumn(name = "province_code")
    private Province province;

    @OneToMany(mappedBy = "district", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Ward> wards;

    public District(String code, String name, Province province) {
        this.code = code;
        this.name = name;
        this.province = province;
    }
}
