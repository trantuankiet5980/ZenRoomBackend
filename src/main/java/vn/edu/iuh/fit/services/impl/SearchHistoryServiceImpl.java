package vn.edu.iuh.fit.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.entities.SearchHistory;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.repositories.SearchHistoryRepository;
import vn.edu.iuh.fit.repositories.UserRepository;
import vn.edu.iuh.fit.services.SearchHistoryService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchHistoryServiceImpl implements SearchHistoryService {
    private final SearchHistoryRepository searchHistoryRepository;
    private final UserRepository userRepository;

    private static final ObjectMapper M = new ObjectMapper()
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    @Override
    @Transactional
    public void saveHistory(String userId, String keyword, ObjectNode filtersJson) {
        if (userId == null) return;

        String filters = null;
        try { filters = (filtersJson != null) ? M.writeValueAsString(filtersJson) : null; }
        catch (Exception ignored) { }

        User me = userRepository.findById(userId).orElse(null);
        if (me == null) return;

        Optional<SearchHistory> existed = searchHistoryRepository
                .findTopByTenant_UserIdAndKeywordAndFiltersOrderByCreatedAtDesc(userId, keyword, filters);

        String finalFilters = filters;
        SearchHistory e = existed.orElseGet(() -> {
            SearchHistory s = new SearchHistory();
            s.setSearchId(UUID.randomUUID().toString());
            s.setTenant(me);
            s.setKeyword(keyword);
            s.setFilters(finalFilters);
            return s;
        });
        e.setCreatedAt(LocalDateTime.now());
        searchHistoryRepository.save(e);
    }
}
