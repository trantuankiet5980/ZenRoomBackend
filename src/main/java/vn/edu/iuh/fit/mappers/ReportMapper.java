package vn.edu.iuh.fit.mappers;

import org.springframework.stereotype.Component;
import vn.edu.iuh.fit.dtos.ReportDto;
import vn.edu.iuh.fit.entities.Report;

@Component
public class ReportMapper {

    private final UserMapper userMapper;
    private final PropertyMapper propertyMapper;
    private final BookingMapper bookingMapper;

    public ReportMapper(UserMapper userMapper,
                        PropertyMapper propertyMapper,
                        BookingMapper bookingMapper) {
        this.userMapper = userMapper;
        this.propertyMapper = propertyMapper;
        this.bookingMapper = bookingMapper;
    }

    /** Entity -> DTO */
    public ReportDto toDto(Report e) {
        if (e == null) return null;
        return new ReportDto(
                e.getReportId(),
                userMapper.toDto(e.getReporter()),   // shallow user
                userMapper.toDto(e.getReported()),
                propertyMapper.toDto(e.getProperty()),
                bookingMapper.toDto(e.getBooking()),
                e.getReason(),
                e.getStatus(),
                e.getCreatedAt()
        );
    }

    /** DTO -> Entity (KHÔNG gắn reporter/reported/property/booking ở đây; gắn trong Service) */
    public Report toEntity(ReportDto d) {
        if (d == null) return null;
        return Report.builder()
                .reportId(d.getReportId())
                .reason(d.getReason())
                .status(d.getStatus())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
