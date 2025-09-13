package vn.edu.iuh.fit.dtos.responses;

import lombok.Value;
import vn.edu.iuh.fit.dtos.FavoriteDto;

import java.util.List;

@Value
public class FavoriteListResponse {
    List<FavoriteDto> favorites;
}
