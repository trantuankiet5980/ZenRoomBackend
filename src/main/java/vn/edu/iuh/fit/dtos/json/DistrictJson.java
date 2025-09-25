package vn.edu.iuh.fit.dtos.json;

import lombok.Data;

@Data
public class DistrictJson {
    private String code;
    private String name;
    private String parent_code;
}