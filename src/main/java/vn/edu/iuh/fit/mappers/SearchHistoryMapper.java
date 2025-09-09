package vn.edu.iuh.fit.mappers;

import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.SearchHistoryDto;
import vn.edu.iuh.fit.entities.SearchHistory;

@Component
public class SearchHistoryMapper {

    private final UserMapper userMapper;

    public SearchHistoryMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /** Entity -> DTO */
    public SearchHistoryDto toDto(SearchHistory e) {
        if (e == null) return null;
        return new SearchHistoryDto(
                e.getSearchId(),
                userMapper.toDto(e.getTenant()),
                e.getKeyword(),
                e.getFilters(),
                e.getCreatedAt()
        );
    }

    /** DTO -> Entity */
    public SearchHistory toEntity(SearchHistoryDto d) {
        if (d == null) return null;
        return SearchHistory.builder()
                .searchId(d.getSearchId())
                .keyword(d.getKeyword())
                .filters(d.getFilters())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
