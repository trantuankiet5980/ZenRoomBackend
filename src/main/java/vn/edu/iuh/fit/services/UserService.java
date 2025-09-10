package vn.edu.iuh.fit.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.dtos.requests.SignUpRequest;
import vn.edu.iuh.fit.dtos.user.UserCreateRequest;
import vn.edu.iuh.fit.dtos.user.UserResponse;
import vn.edu.iuh.fit.dtos.user.UserUpdateRequest;
import vn.edu.iuh.fit.entities.User;

import java.util.List;

@Service
public interface UserService {
    UserResponse create(UserCreateRequest req);
    UserResponse getById(String id);
    Page<UserResponse> list(Pageable pageable);
    UserResponse update(String id, UserUpdateRequest req);
    void delete(String id);

    void requestDeleteAccount(String userId);
}
