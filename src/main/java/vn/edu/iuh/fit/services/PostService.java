package vn.edu.iuh.fit.services;

import vn.edu.iuh.fit.dtos.PostCreateDTO;
import vn.edu.iuh.fit.entities.Post;

public interface PostService {
    Post create(PostCreateDTO dto);
}
