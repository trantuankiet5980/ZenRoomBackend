package vn.edu.iuh.fit.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ward {
    @Id
    private String code;
    private String name_with_type;

    @ManyToOne
    @JoinColumn(name = "district_code")
    private District district;
}
