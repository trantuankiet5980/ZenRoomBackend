package vn.edu.iuh.fit.services.impl;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.iuh.fit.entities.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PropertySpecs {
    public static Specification<Property> landlordIdEq(String landlordId) {
        return (root, cq, cb) -> {
            if (landlordId == null || landlordId.isBlank()) return cb.conjunction();
            return cb.equal(root.get("landlord").get("userId"), landlordId);
        };
    }

    public static Specification<Property> postStatusEq(String postStatus) {
        return (root, cq, cb) -> {
            if (postStatus == null || postStatus.isBlank()) return cb.conjunction();
            return cb.equal(root.get("postStatus"), postStatus);
        };
    }

    public static Specification<Property> typeEq(String type) {
        return (root, cq, cb) -> {
            if (type == null || type.isBlank()) return cb.conjunction();
            return cb.equal(root.get("propertyType"), type);
        };
    }

    public static Specification<Property> keywordLike(String keyword) {
        return (root, cq, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }

    // Thêm lọc theo province
    public static Specification<Property> provinceCodeEq(String provinceCode) {
        return (root, cq, cb) -> {
            if (provinceCode == null || provinceCode.isBlank()) return cb.conjunction();
            Join<Property, Address> addressJoin = root.join("address", JoinType.LEFT);
            Join<Address, Province> provinceJoin = addressJoin.join("province", JoinType.LEFT);
            return cb.equal(provinceJoin.get("code"), provinceCode);
        };
    }

    // Thêm lọc theo district
    public static Specification<Property> districtCodeEq(String districtCode) {
        return (root, cq, cb) -> {
            if (districtCode == null || districtCode.isBlank()) return cb.conjunction();
            Join<Property, Address> addressJoin = root.join("address", JoinType.LEFT);
            Join<Address, District> districtJoin = addressJoin.join("district", JoinType.LEFT);
            return cb.equal(districtJoin.get("code"), districtCode);
        };
    }

    public static Specification<Property> createdAtBetween(LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate;
        LocalDate end = endDate;

        if (start != null && end != null && start.isAfter(end)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }

        final LocalDate finalStart = start;
        final LocalDate finalEnd = end;

        return (root, cq, cb) -> {
            if (finalStart == null && finalEnd == null) {
                return cb.conjunction();
            }

            if (finalStart != null && finalEnd != null) {
                LocalDateTime from = finalStart.atStartOfDay();
                LocalDateTime toExclusive = finalEnd.plusDays(1).atStartOfDay();
                return cb.and(
                        cb.greaterThanOrEqualTo(root.get("createdAt"), from),
                        cb.lessThan(root.get("createdAt"), toExclusive)
                );
            }

            if (finalStart != null) {
                LocalDateTime from = finalStart.atStartOfDay();
                return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            }

            LocalDateTime toExclusive = finalEnd.plusDays(1).atStartOfDay();
            return cb.lessThan(root.get("createdAt"), toExclusive);
        };
    }
}
