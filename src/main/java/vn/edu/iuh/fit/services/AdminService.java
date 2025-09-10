package vn.edu.iuh.fit.services;

import vn.edu.iuh.fit.entities.User;

public interface AdminService {
    void processDeletionRequest(String userId, boolean approve, String reason);
    void banUser(String userId, String reason);
    void unbanUser(String userId);
    void hardDelete(String userId);
}
