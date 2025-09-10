package vn.edu.iuh.fit.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.dtos.UserDto;
import vn.edu.iuh.fit.dtos.requests.SignUpRequest;
import vn.edu.iuh.fit.dtos.user.UserCreateRequest;
import vn.edu.iuh.fit.dtos.user.UserResponse;
import vn.edu.iuh.fit.dtos.user.UserUpdateRequest;
import vn.edu.iuh.fit.entities.User;

import java.util.List;

@Service
public interface UserService {
    UserDto create(UserCreateRequest req);
    Page<UserResponse> list(Pageable pageable);
    UserDto updateMe(UserDto dto);
    UserDto getById(String id);
    UserDto update(String id, UserDto dto);
    void requestDeleteAccount(String userId);
}
