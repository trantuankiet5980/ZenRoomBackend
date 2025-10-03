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

    @Query(value = """
            select p.* from properties p
            where p.property_id <> :propertyId
              and p.post_status = 'APPROVED'
              and p.embedding is not null
              and (select embedding from properties where property_id = :propertyId) is not null
            order by p.embedding <-> (select embedding from properties where property_id = :propertyId)
            limit :limit
            """, nativeQuery = true)
    List<Property> findSimilarByEmbedding(@Param("propertyId") String propertyId, @Param("limit") int limit);

    List<Property> findByPostStatusOrderByPublishedAtDesc(PostStatus status, Pageable pageable);

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
