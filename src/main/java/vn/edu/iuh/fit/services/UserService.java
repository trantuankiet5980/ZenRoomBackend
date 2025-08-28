package vn.edu.iuh.fit.services;

import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.entities.User;

import java.util.List;

@Service
public interface UserService {
    User create(User user);
    User getById(String id);
    List<User> getAll();
    User update(String id, User user);
    void delete(String id);
}
