package vn.edu.iuh.fit.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.iuh.fit.entities.UserEvent;
import vn.edu.iuh.fit.entities.enums.EventType;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface UserEventRepository extends JpaRepository<UserEvent, String> {
    @Query("""
            select e.property.propertyId as propertyId,
                   sum(case
                           when e.eventType = vn.edu.iuh.fit.entities.enums.EventType.BOOKING then 6
                           when e.eventType = vn.edu.iuh.fit.entities.enums.EventType.FAVORITE then 3
                           when e.eventType = vn.edu.iuh.fit.entities.enums.EventType.CLICK then 2
                           else 1
                       end) as score,
                   max(e.occurredAt) as lastSeen
            from UserEvent e
            where e.user.userId = :userId and e.property.propertyId is not null
            group by e.property.propertyId
            order by lastSeen desc
            """)
    List<UserPropertyScore> findUserPropertyScores(@Param("userId") String userId, Pageable pageable);

    @Query("""
            select e.property.propertyId as propertyId,
                   sum(case 
                           when e.eventType = vn.edu.iuh.fit.entities.enums.EventType.BOOKING then 6
                           when e.eventType = vn.edu.iuh.fit.entities.enums.EventType.FAVORITE then 3
                           when e.eventType = vn.edu.iuh.fit.entities.enums.EventType.CLICK then 2
                           else 1
                       end) as score
            from UserEvent e
            where e.property.propertyId is not null and e.occurredAt >= :threshold
            group by e.property.propertyId
            order by score desc
            """)
    List<PropertyScore> findPopularSince(@Param("threshold") LocalDateTime threshold, Pageable pageable);

    @Query("""
            select e2.property.propertyId as propertyId,
                   count(e2) as score
            from UserEvent e1
            join UserEvent e2 on e1.user = e2.user
            where e1.property.propertyId = :propertyId
              and e2.property.propertyId <> :propertyId
              and e1.eventType in :eventTypes
              and e2.eventType in :eventTypes
              and e1.occurredAt >= :threshold
              and e2.occurredAt >= :threshold
            group by e2.property.propertyId
            order by score desc
            """)
    List<PropertyScore> findCoVisitedProperties(@Param("propertyId") String propertyId,
                                                @Param("eventTypes") Collection<EventType> eventTypes,
                                                @Param("threshold") LocalDateTime threshold,
                                                Pageable pageable);

    @Query("""
            select e.property.propertyId as propertyId,
                   max(e.occurredAt) as lastSeen
            from UserEvent e
            where e.user.userId = :userId
              and e.property.propertyId is not null
              and e.eventType in :eventTypes
            group by e.property.propertyId
            order by max(e.occurredAt) desc
            """)
    List<RecentPropertyProjection> findRecentProperties(@Param("userId") String userId,
                                                        @Param("eventTypes") Collection<EventType> eventTypes,
                                                        Pageable pageable);

    interface PropertyScore {
        String getPropertyId();
        Double getScore();
    }

    interface UserPropertyScore extends PropertyScore {
        LocalDateTime getLastSeen();
    }
    interface RecentPropertyProjection {
        String getPropertyId();
        LocalDateTime getLastSeen();
    }
}
