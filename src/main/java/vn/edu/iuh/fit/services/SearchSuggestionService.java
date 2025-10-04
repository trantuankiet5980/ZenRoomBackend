package vn.edu.iuh.fit.services;

import vn.edu.iuh.fit.dtos.SearchSuggestionDto;
import vn.edu.iuh.fit.entities.Property;

import java.util.List;

public interface SearchSuggestionService {
    List<SearchSuggestionDto> suggest(String query, int limit);

    void upsertPropertySuggestion(Property property);

    void removePropertySuggestion(String propertyId);

    void rebuildPropertySuggestions();

    void recordQuery(String query, String suggestionId);

    void recordClick(String query, String suggestionId);
}
