package vn.edu.iuh.fit.services.embedding.impl;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.collection.spi.PersistentCollection;
import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.entities.Property;
import vn.edu.iuh.fit.entities.enums.PropertyType;
import vn.edu.iuh.fit.services.embedding.PropertyEmbeddingGenerator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;


@Slf4j
@Component
public class HeuristicPropertyEmbeddingGenerator implements PropertyEmbeddingGenerator {

    private static final double MAX_AREA = 300.0; // Giới hạn chuẩn hóa diện tích (m2)
    private static final double MAX_PRICE = 50_000_000.0; // Giới hạn chuẩn hóa giá thuê (VND)
    private static final double MAX_DEPOSIT = 100_000_000.0; // Giới hạn chuẩn hóa tiền cọc (VND)
    private static final double MAX_CAPACITY = 10.0; // Giới hạn chuẩn hóa sức chứa
    private static final double MAX_ROOM_COUNT = 10.0; // Giới hạn chuẩn hóa số phòng ngủ/vệ sinh
    private static final double MAX_FLOOR = 20.0; // Giới hạn chuẩn hóa số tầng
    private static final double MAX_PARKING = 10.0; // Giới hạn chuẩn hóa số chỗ đậu xe
    private static final double MAX_TEXT_LENGTH = 500.0; // Giới hạn chuẩn hóa chiều dài mô tả

    @Override
    public Optional<double[]> generate(Property property) {
        if (property == null) {
            // Không có dữ liệu để xử lý
            return Optional.empty();
        }

        double areaScore = normalize(property.getArea(), MAX_AREA); // Điểm diện tích
        double priceScore = normalize(property.getPrice(), MAX_PRICE); // Điểm giá thuê
        double depositScore = normalize(property.getDeposit(), MAX_DEPOSIT); // Điểm tiền cọc
        double capacityScore = normalize(property.getCapacity(), MAX_CAPACITY); // Điểm sức chứa
        double bedroomScore = normalize(property.getBedrooms(), MAX_ROOM_COUNT); // Điểm số phòng ngủ
        double bathroomScore = normalize(property.getBathrooms(), MAX_ROOM_COUNT); // Điểm số phòng vệ sinh
        double floorScore = normalize(property.getFloorNo(), MAX_FLOOR); // Điểm số tầng
        double parkingScore = normalize(property.getParkingSlots(), MAX_PARKING); // Điểm chỗ đậu xe

        double titleScore = encodeText(property.getTitle(), 120); // Điểm tiêu đề
        double descriptionScore = encodeText(property.getDescription(), MAX_TEXT_LENGTH); // Điểm mô tả
        double serviceScore = encodeCollection(property.getServices()); // Điểm tiện ích đi kèm
        double furnishingScore = encodeCollection(property.getFurnishings()); // Điểm nội thất đi kèm
        double addressScore = property.getAddress() != null ? 1.0 : 0.0; // Điểm có địa chỉ
        double typeScore = property.getPropertyType() == PropertyType.BUILDING ? 1.0 : 0.0; // Điểm loại BUILDING
        double roomFlag = property.getPropertyType() == PropertyType.ROOM ? 1.0 : 0.0; // Điểm loại ROOM

        double[] vector = new double[] {
                areaScore,
                priceScore,
                depositScore,
                capacityScore,
                bedroomScore,
                bathroomScore,
                floorScore,
                parkingScore,
                titleScore,
                descriptionScore,
                serviceScore,
                furnishingScore,
                addressScore,
                typeScore,
                roomFlag
        };

        boolean hasSignal = false; // Kiểm tra vector có dữ liệu
        for (double value : vector) {
            if (value > 0.0) {
                hasSignal = true;
                break;
            }
        }

        if (!hasSignal) {
            log.debug("No heuristic signal for property {}", property.getPropertyId()); // Ghi log khi thiếu dữ liệu
            return Optional.empty();
        }

        return Optional.of(vector); // Trả về vector đã tính
    }

    private double normalize(Double value, double max) {
        if (value == null || value <= 0 || max <= 0) {
            // Không có dữ liệu hoặc dữ liệu không hợp lệ
            return 0.0;
        }
        return Math.min(value / max, 1.0); // Giới hạn trong [0,1]
    }

    private double normalize(Integer value, double max) {
        if (value == null || value <= 0 || max <= 0) {
            // Không có dữ liệu hoặc dữ liệu không hợp lệ
            return 0.0;
        }
        return Math.min(value / max, 1.0); // Giới hạn trong [0,1]
    }

    private double normalize(BigDecimal value, double max) {
        if (value == null) {
            // Không có dữ liệu
            return 0.0;
        }
        return normalize(value.doubleValue(), max); // Chuyển BigDecimal sang double và chuẩn hóa
    }

    private double encodeText(String text, double maxLength) {
        if (text == null || text.isBlank()) {
            // Không có nội dung văn bản
            return 0.0;
        }
        double length = text.trim().length(); // Lấy độ dài nội dung
        return Math.min(length / maxLength, 1.0); // Giới hạn trong [0,1]
    }

    private double encodeCollection(List<?> items) {
        if (items == null) {
            // Không có danh sách để đánh giá
            return 0.0;
        }
        if (!Hibernate.isInitialized(items)) {
            // Bộ sưu tập đang ở trạng thái LAZY, tránh kích hoạt session tạm
            return 0.0;
        }

        if (items instanceof PersistentCollection persistentCollection) {
            if (!persistentCollection.wasInitialized()) {
                log.debug("Skip uninitialized persistent collection {}", items.getClass().getName());
                return 0.0;
            }
        }

        try {
            if (items.isEmpty()) {
                // Không có phần tử trong danh sách
                return 0.0;
            }
            return Math.min(items.size() / 10.0, 1.0); // Chuẩn hóa dựa trên kích thước danh sách
        } catch (LazyInitializationException ex) {
            log.debug("Cannot inspect lazy collection {} without an open session", items.getClass().getName(), ex);
            return 0.0;
        }
    }
}
