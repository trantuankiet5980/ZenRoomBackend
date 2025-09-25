package vn.edu.iuh.fit.dtos;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WardDto {
    private String code;
    private String name;
    private String parent_code;
}
