package vn.edu.iuh.fit.services;

import vn.edu.iuh.fit.auths.UserPrincipal;
import vn.edu.iuh.fit.dtos.requests.PostRejectRequest;
import vn.edu.iuh.fit.entities.Post;

import java.util.List;

public interface PostModerationService {
    void approve(String postId, UserPrincipal actor);
    void reject(String postId, PostRejectRequest req, UserPrincipal actor);
}
