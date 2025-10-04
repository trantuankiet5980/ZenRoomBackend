package vn.edu.iuh.fit.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.entities.SearchQueryLog;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchQueryLogRepository extends JpaRepository<SearchQueryLog, String> {

    Optional<SearchQueryLog> findBySuggestionIdAndNormalizedQuery(String suggestionId, String normalizedQuery);

    @Query("select l from SearchQueryLog l where l.suggestionId is null and l.normalizedQuery = :normalizedQuery")
    Optional<SearchQueryLog> findByNormalizedQueryWithoutSuggestion(@Param("normalizedQuery") String normalizedQuery);

    List<SearchQueryLog> findTop50BySuggestionIdIsNotNullOrderByLastOccurredAtDesc();
}
