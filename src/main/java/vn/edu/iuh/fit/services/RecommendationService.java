package vn.edu.iuh.fit.services;

import vn.edu.iuh.fit.dtos.PropertyDto;

import java.util.List;

public interface RecommendationService {
    List<PropertyDto> getSimilarRooms(String roomId, int limit, String userId);

    List<PropertyDto> getPersonalRecommendations(String userId, int limit);

    List<PropertyDto> rerankAfterSearch(String query, int limit, String userId);
}
