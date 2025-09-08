package vn.edu.iuh.fit.services;

import vn.edu.iuh.fit.dtos.PostDto;
import vn.edu.iuh.fit.entities.Post;

public interface PostService {
    Post create(PostDto dto);

}
