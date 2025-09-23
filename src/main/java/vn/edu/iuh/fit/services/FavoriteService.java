package vn.edu.iuh.fit.services;

import vn.edu.iuh.fit.dtos.FavoriteDto;
import vn.edu.iuh.fit.dtos.requests.FavoriteRequest;

import java.util.List;

public interface FavoriteService {
    FavoriteDto addFavorite(FavoriteRequest request);
    List<FavoriteDto> getFavorites();
    void removeFavorite(String propertyId);
    void removeAllFavorites();
}
