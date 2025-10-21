package vn.edu.iuh.fit.repositories.impl;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.dtos.*;
import vn.edu.iuh.fit.repositories.AdminStatsRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AdminStatsRepositoryImpl implements AdminStatsRepository {

    private final EntityManager em;

    @Override
    public OverviewStatsDTO getOverview(Integer year, Integer month) {
        //Users
        var totalUsers = ((Number) em.createNativeQuery("""
            SELECT COUNT(*) FROM users
        """).getSingleResult()).longValue();
        var activeUsers       = ((Number) em.createNativeQuery("""
            SELECT COUNT(*) FROM users WHERE status = 'ACTIVE'
        """).getSingleResult()).longValue();

        var landlords         = ((Number) em.createNativeQuery("""
            SELECT COUNT(*) FROM users u 
            JOIN roles r ON r.role_id = u.role_id
            WHERE r.role_name = 'LANDLORD'
        """).getSingleResult()).longValue();

        var tenants           = ((Number) em.createNativeQuery("""
            SELECT COUNT(*) FROM users u 
            JOIN roles r ON r.role_id = u.role_id
            WHERE r.role_name = 'TENANT'
        """).getSingleResult()).longValue();

        // Properties
        var totalProperties   = ((Number) em.createNativeQuery("""
            SELECT COUNT(*) FROM properties
        """).getSingleResult()).longValue();

        var approvedProps     = ((Number) em.createNativeQuery("""
            SELECT COUNT(*) FROM properties WHERE post_status = 'APPROVED'
        """).getSingleResult()).longValue();

        var pendingProps      = ((Number) em.createNativeQuery("""
            SELECT COUNT(*) FROM properties WHERE post_status = 'PENDING'
        """).getSingleResult()).longValue();

        // Bookings
        var totalBookings     = ((Number) em.createNativeQuery("""
            SELECT COUNT(*) FROM bookings
        """).getSingleResult()).longValue();

        var completedBookings = ((Number) em.createNativeQuery("""
            SELECT COUNT(*) FROM bookings WHERE booking_status = 'COMPLETED'
        """).getSingleResult()).longValue();

        var cancelledBookings = ((Number) em.createNativeQuery("""
            SELECT COUNT(*) FROM bookings WHERE booking_status = 'CANCELLED'
        """).getSingleResult()).longValue();

        BigDecimal totalRevenue = (BigDecimal) em.createNativeQuery("""
            SELECT COALESCE(SUM(i.total), 0)
            FROM invoice i
            WHERE i.status = 'PAID'
        """).getSingleResult();

        return new OverviewStatsDTO(
                totalUsers, activeUsers, landlords, tenants,
                totalProperties, approvedProps, pendingProps,
                totalBookings, completedBookings, cancelledBookings,
                totalRevenue
        );
    }

    @Override
    public BigDecimal getRevenueLastNDays(int days) {
        // Ưu tiên sum từ invoice.total với status = 'PAID'
        var q = em.createNativeQuery("""
            SELECT COALESCE(SUM(i.total), 0)
            FROM invoice i
            WHERE i.status = 'PAID'
              AND i.paid_at >= (NOW() - INTERVAL ?1 DAY)
        """);
        q.setParameter(1, days);
        BigDecimal sum = (BigDecimal) q.getSingleResult();
        if (sum != null && sum.compareTo(BigDecimal.ZERO) > 0) return sum;

        // Fallback: sum theo payments SUCCESS (nếu bạn dùng bảng payments)
        var q2 = em.createNativeQuery("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM payments p
            WHERE p.payment_status = 'SUCCESS'
              AND p.created_at >= (NOW() - INTERVAL ?1 DAY)
        """);
        q2.setParameter(1, days);
        return (BigDecimal) q2.getSingleResult();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<DailyRevenueDTO> getDailyRevenueLastNDays(int days) {
        var rows = em.createNativeQuery("""
            SELECT DATE(COALESCE(i.paid_at, i.issued_at)) AS d,
                   COALESCE(SUM(i.total), 0) AS rev
            FROM invoice i
            WHERE i.status = 'PAID'
              AND COALESCE(i.paid_at, i.issued_at) >= (CURDATE() - INTERVAL ?1 DAY)
            GROUP BY DATE(COALESCE(i.paid_at, i.issued_at))
            ORDER BY d ASC
        """).setParameter(1, days).getResultList();

        List<DailyRevenueDTO> out = new ArrayList<>();
        for (Object r : rows) {
            Object[] a = (Object[]) r;
            out.add(new DailyRevenueDTO(
                    ((java.sql.Date) a[0]).toLocalDate(),
                    (BigDecimal) a[1]
            ));
        }
        return out;
    }

    @Override
    public BigDecimal getRevenueForDay(LocalDate date) {
        return (BigDecimal) em.createNativeQuery("""
            SELECT COALESCE(SUM(i.total), 0)
            FROM invoice i
            WHERE i.status = 'PAID'
              AND DATE(COALESCE(i.paid_at, i.issued_at)) = ?1
        """)
                .setParameter(1, java.sql.Date.valueOf(date))
                .getSingleResult();
    }

    @Override
    public BigDecimal getRevenueForMonth(int year, int month) {
        return (BigDecimal) em.createNativeQuery("""
            SELECT COALESCE(SUM(i.total), 0)
            FROM invoice i
            WHERE i.status = 'PAID'
              AND YEAR(COALESCE(i.paid_at, i.issued_at)) = ?1
              AND MONTH(COALESCE(i.paid_at, i.issued_at)) = ?2
        """)
                .setParameter(1, year)
                .setParameter(2, month)
                .getSingleResult();
    }

    @Override
    public BigDecimal getRevenueForYear(int year) {
        return (BigDecimal) em.createNativeQuery("""
            SELECT COALESCE(SUM(i.total), 0)
            FROM invoice i
            WHERE i.status = 'PAID'
              AND YEAR(COALESCE(i.paid_at, i.issued_at)) = ?1
        """)
                .setParameter(1, year)
                .getSingleResult();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<DailyRevenueDTO> getDailyRevenueForMonth(int year, int month) {
        var rows = em.createNativeQuery("""
            SELECT DATE(COALESCE(i.paid_at, i.issued_at)) AS d,
                   COALESCE(SUM(i.total), 0) AS rev
            FROM invoice i
            WHERE i.status = 'PAID'
              AND YEAR(COALESCE(i.paid_at, i.issued_at)) = ?1
              AND MONTH(COALESCE(i.paid_at, i.issued_at)) = ?2
            GROUP BY DATE(COALESCE(i.paid_at, i.issued_at))
            ORDER BY d ASC
        """)
                .setParameter(1, year)
                .setParameter(2, month)
                .getResultList();

        List<DailyRevenueDTO> out = new ArrayList<>();
        for (Object r : rows) {
            Object[] a = (Object[]) r;
            out.add(new DailyRevenueDTO(
                    ((java.sql.Date) a[0]).toLocalDate(),
                    (BigDecimal) a[1]
            ));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<MonthlyRevenueDTO> getMonthlyRevenueForYear(int year) {
        var rows = em.createNativeQuery("""
            SELECT YEAR(COALESCE(i.paid_at, i.issued_at)) AS y,
                   MONTH(COALESCE(i.paid_at, i.issued_at)) AS m,
                   COALESCE(SUM(i.total), 0) AS rev
            FROM invoice i
            WHERE i.status = 'PAID'
              AND YEAR(COALESCE(i.paid_at, i.issued_at)) = ?1
            GROUP BY y, m
            ORDER BY y, m
        """)
                .setParameter(1, year)
                .getResultList();

        List<MonthlyRevenueDTO> out = new ArrayList<>();
        for (Object row : rows) {
            Object[] a = (Object[]) row;
            out.add(new MonthlyRevenueDTO(
                    ((Number) a[0]).intValue(),
                    ((Number) a[1]).intValue(),
                    (BigDecimal) a[2]
            ));
        }
        return out;
    }

    @Override
    public long getApprovedPostCountForDay(LocalDate date) {
        return ((Number) em.createNativeQuery("""
            SELECT COUNT(*)
            FROM properties p
            WHERE p.post_status = 'APPROVED'
              AND DATE(p.created_at) = ?1
        """)
                .setParameter(1, java.sql.Date.valueOf(date))
                .getSingleResult()).longValue();
    }

    @Override
    public long getApprovedPostCountForMonth(int year, int month) {
        return ((Number) em.createNativeQuery("""
            SELECT COUNT(*)
            FROM properties p
            WHERE p.post_status = 'APPROVED'
              AND YEAR(p.created_at) = ?1
              AND MONTH(p.created_at) = ?2
        """)
                .setParameter(1, year)
                .setParameter(2, month)
                .getSingleResult()).longValue();
    }

    @Override
    public long getApprovedPostCountForYear(int year) {
        return ((Number) em.createNativeQuery("""
            SELECT COUNT(*)
            FROM properties p
            WHERE p.post_status = 'APPROVED'
              AND YEAR(p.created_at) = ?1
        """)
                .setParameter(1, year)
                .getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<DailyPostCountDTO> getApprovedPostDailyCountsForMonth(int year, int month) {
        var rows = em.createNativeQuery("""
            SELECT DATE(p.created_at) AS d,
                   COUNT(*) AS total
            FROM properties p
            WHERE p.post_status = 'APPROVED'
              AND YEAR(p.created_at) = ?1
              AND MONTH(p.created_at) = ?2
            GROUP BY DATE(p.created_at)
            ORDER BY d ASC
        """)
                .setParameter(1, year)
                .setParameter(2, month)
                .getResultList();

        List<DailyPostCountDTO> out = new ArrayList<>();
        for (Object row : rows) {
            Object[] a = (Object[]) row;
            out.add(new DailyPostCountDTO(
                    ((java.sql.Date) a[0]).toLocalDate(),
                    ((Number) a[1]).longValue()
            ));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<MonthlyPostCountDTO> getApprovedPostMonthlyCountsForYear(int year) {
        var rows = em.createNativeQuery("""
            SELECT YEAR(p.created_at) AS y,
                   MONTH(p.created_at) AS m,
                   COUNT(*) AS total
            FROM properties p
            WHERE p.post_status = 'APPROVED'
              AND YEAR(p.created_at) = ?1
            GROUP BY y, m
            ORDER BY y, m
        """)
                .setParameter(1, year)
                .getResultList();

        List<MonthlyPostCountDTO> out = new ArrayList<>();
        for (Object row : rows) {
            Object[] a = (Object[]) row;
            out.add(new MonthlyPostCountDTO(
                    ((Number) a[0]).intValue(),
                    ((Number) a[1]).intValue(),
                    ((Number) a[2]).longValue()
            ));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<RecentBookingDTO> getRecentBookings(int limit) {
        var rows = em.createNativeQuery("""
            SELECT b.booking_id,
                   u.full_name AS tenant_name,
                   p.title     AS property_title,
                   b.total_price,
                   b.booking_status,
                   b.created_at
            FROM bookings b
            JOIN users u ON u.user_id = b.tenant_id
            JOIN properties p ON p.property_id = b.property_id
            ORDER BY b.created_at DESC
            LIMIT ?1
        """).setParameter(1, Math.max(1, Math.min(limit, 100))).getResultList();

        List<RecentBookingDTO> out = new ArrayList<>();
        for (Object r : rows) {
            Object[] a = (Object[]) r;
            out.add(new RecentBookingDTO(
                    (String) a[0],
                    (String) a[1],
                    (String) a[2],
                    (BigDecimal) a[3],
                    (String) a[4],
                    ((java.sql.Timestamp) a[5]).toLocalDateTime()
            ));
        }
        return out;
    }
}
