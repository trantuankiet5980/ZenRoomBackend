package vn.edu.iuh.fit.dtos;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DistrictDto {
    private String code;
    private String name;
    private String parent_code;
}
