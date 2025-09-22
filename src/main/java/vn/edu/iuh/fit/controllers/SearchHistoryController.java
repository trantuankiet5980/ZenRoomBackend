package vn.edu.iuh.fit.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.SearchHistoryDto;
import vn.edu.iuh.fit.entities.SearchHistory;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.mappers.SearchHistoryMapper;
import vn.edu.iuh.fit.repositories.SearchHistoryRepository;
import vn.edu.iuh.fit.repositories.UserRepository;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/search-history")
@RequiredArgsConstructor
public class SearchHistoryController {
    private final SearchHistoryRepository searchHistoryRepository;
    private final UserRepository userRepository;
    private final SearchHistoryMapper searchHistoryMapper;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    // 1) Lưu lịch sử (upsert theo (keyword, filters) — nếu trùng thì update created_at)
    @PostMapping
    @Transactional
    public SearchHistoryDto save(Principal principal, @RequestBody SaveReq req) {
        String userId = principal.getName();
        if (req.keyword() != null && req.keyword().length() > 255)
            throw new IllegalArgumentException("keyword too long (<=255)");

        // Chuẩn hóa filters -> JSON string có thứ tự khóa ổn định
        String filtersJson = normalizeJson(req.filters());

        User me = userRepository.findById(userId).orElseThrow();

        Optional<SearchHistory> existed = searchHistoryRepository
                .findTopByTenant_UserIdAndKeywordAndFiltersOrderByCreatedAtDesc(
                        userId, req.keyword(), filtersJson);

        SearchHistory entity = existed.orElseGet(() -> {
            SearchHistory s = new SearchHistory();
            s.setSearchId(UUID.randomUUID().toString());
            s.setTenant(me);
            s.setKeyword(req.keyword());
            s.setFilters(filtersJson);
            return s;
        });
        entity.setCreatedAt(LocalDateTime.now()); // “refresh” thời gian
        SearchHistory saved = searchHistoryRepository.save(entity);
        return searchHistoryMapper.toDto(saved);
    }

    // 2) Danh sách lịch sử của current user (phân trang, mới nhất trước)
    @GetMapping
    public Page<SearchHistoryDto> list(Principal principal,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        String userId = principal.getName();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return searchHistoryRepository.findByTenant_UserIdOrderByCreatedAtDesc(userId, pageable)
                .map(searchHistoryMapper::toDto);
    }

    // 3) Xóa 1 bản ghi (chỉ xóa của chính mình)
    @DeleteMapping("/{id}")
    @Transactional
    public void deleteOne(@PathVariable String id, Principal principal) {
        searchHistoryRepository.deleteOneByOwner(principal.getName(), id);
    }

    // 4) Xóa toàn bộ lịch sử của current user
    @DeleteMapping("/clear")
    @Transactional
    public void clearMine(Principal principal) {
        searchHistoryRepository.deleteAllByOwner(principal.getName());
    }

    // 5) Top keyword gần đây (duy nhất)
    @GetMapping("/recent-keywords")
    public List<String> recentKeywords(Principal principal,
                                       @RequestParam(defaultValue = "10") int limit) {
        return searchHistoryRepository.recentKeywords(principal.getName(),
                PageRequest.of(0, Math.max(1, Math.min(limit, 50))));
    }

    private String normalizeJson(ObjectNode node) {
        if (node == null) return null;
        try {
            // sort keys -> string ổn định để so sánh
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("filters is not valid JSON");
        }
    }
    public record SaveReq(String keyword, ObjectNode filters) {}
}
