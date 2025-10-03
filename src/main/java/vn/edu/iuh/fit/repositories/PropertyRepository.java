package vn.edu.iuh.fit.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.iuh.fit.entities.Property;
import vn.edu.iuh.fit.entities.enums.PostStatus;
import vn.edu.iuh.fit.entities.enums.PropertyType;

import java.util.List;
import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, String>, JpaSpecificationExecutor<Property> {

    @EntityGraph(attributePaths = {
            "landlord",
            "address",
            "furnishings",
            "furnishings.furnishing",
            "media"
    })
    Optional<Property> findWithDetailsByPropertyId(String propertyId);

    List<Property> findTop200ByPostStatusAndPropertyTypeAndPropertyIdNotOrderByCreatedAtDesc(
            PostStatus status,
            PropertyType propertyType,
            String propertyId
    );

    List<Property> findByPropertyIdNotAndPostStatusAndEmbeddingIsNotNull(
            String propertyId,
            PostStatus postStatus,
            Pageable pageable
    );

    List<Property> findByPostStatusOrderByPublishedAtDesc(PostStatus status, Pageable pageable);

    @Query("""
            select p from Property p
            where p.postStatus = :status
              and (p.embedding is null or trim(p.embedding) = '')
            order by p.updatedAt desc
            """)
    List<Property> findApprovedWithoutEmbedding(@Param("status") PostStatus status, Pageable pageable);

    @Query("""
            select p from Property p
            where p.postStatus = vn.edu.iuh.fit.entities.enums.PostStatus.APPROVED
              and (:keyword is null or :keyword = ''
                   or lower(p.title) like lower(concat('%', :keyword, '%'))
                   or lower(p.description) like lower(concat('%', :keyword, '%'))
                   or lower(p.buildingName) like lower(concat('%', :keyword, '%')))
            order by p.publishedAt desc nulls last, p.createdAt desc
            """)
    List<Property> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
