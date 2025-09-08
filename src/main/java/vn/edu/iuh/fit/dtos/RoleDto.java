package vn.edu.iuh.fit.dtos;

import lombok.Value;

import java.io.Serializable;
import java.util.List;

/**
 * DTO for {@link vn.edu.iuh.fit.entities.Role}
 */
@Value
public class RoleDto implements Serializable {
    String roleId;
    String roleName;
    List<UserDto> users;
}