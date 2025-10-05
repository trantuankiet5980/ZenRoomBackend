package vn.edu.iuh.fit.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.entities.SearchSuggestion;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchSuggestionRepository extends JpaRepository<SearchSuggestion, String> {

    //lay ra de match voi search box
    Optional<SearchSuggestion> findFirstByActiveTrueAndNormalizedText(String normalizedText);

    //lay ra de goi y search suggestion - bat dau bang query
    List<SearchSuggestion> findByActiveTrueAndNormalizedTextStartingWith(String normalizedText, Pageable pageable);

    //lay ra de goi y search suggestion - chua query o bat ky vi tri nao
    List<SearchSuggestion> findByActiveTrueAndNormalizedTextContaining(String normalizedText, Pageable pageable);

    @Query("""
            select s from SearchSuggestion s
            where s.active = true and (
                s.normalizedTerms like concat(:token, '%')
                or s.normalizedTerms like concat('% ', :token, '%')
            )
            """)
    List<SearchSuggestion> findActiveByTermPrefix(@Param("token") String token, Pageable pageable);

    @Query("""
            select s from SearchSuggestion s
            where s.active = true and (
                s.normalizedText like concat('%', :token, '%')
                or s.normalizedTerms like concat('%', :token, '%')
            )
            """)
    List<SearchSuggestion> findActiveByToken(@Param("token") String token, Pageable pageable);

    Optional<SearchSuggestion> findByReferenceTypeAndReferenceId(String referenceType, String referenceId);

    void deleteByReferenceTypeAndReferenceId(String referenceType, String referenceId);
}
