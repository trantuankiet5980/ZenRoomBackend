package vn.edu.iuh.fit.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.iuh.fit.entities.SearchHistory;

import java.util.List;
import java.util.Optional;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, String> {
    Page<SearchHistory> findByTenant_UserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    Optional<SearchHistory> findTopByTenant_UserIdAndKeywordAndFiltersOrderByCreatedAtDesc(
            String userId, String keyword, String filters);

    @Modifying
    @Query("delete from SearchHistory s where s.tenant.userId = :userId and s.searchId = :id")
    void deleteOneByOwner(@Param("userId") String userId, @Param("id") String id);

    @Modifying @Query("delete from SearchHistory s where s.tenant.userId = :userId")
    void deleteAllByOwner(@Param("userId") String userId);

    // recent unique keywords
    @Query("""
        select s.keyword from SearchHistory s
        where s.tenant.userId = :userId and s.keyword is not null
        group by s.keyword
        order by max(s.createdAt) desc
        """)
    List<String> recentKeywords(@Param("userId") String userId, Pageable pageable);
}
