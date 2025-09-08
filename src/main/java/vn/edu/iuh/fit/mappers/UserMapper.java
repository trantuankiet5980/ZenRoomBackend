package vn.edu.iuh.fit.mappers;

import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.UserDto;
import vn.edu.iuh.fit.entities.User;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserMapper {
    public UserDto toDto(User u) {
        if (u == null) return null;
        return new UserDto(
                u.getUserId(),
                u.getFullName(),
                u.getPhoneNumber(),
                u.getEmail(),
                u.getAvatarUrl(),
                u.getLastLogin(),
                u.getStatus(),
                u.getCreatedAt(),
                u.getUpdatedAt()
        );
    }
    public List<UserDto> toDtoList(List<User> users) {
        return users.stream().map(this::toDto).collect(Collectors.toList());
    }
}
