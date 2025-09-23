package vn.edu.iuh.fit.entities.enums;

public enum ChargeBasis {
    FIXED,       // phí cố định 1 lần (dọn phòng, vệ sinh)
    PER_PERSON,  // thu theo số người (khách thêm)
    PER_ROOM,    // theo số phòng (nếu thuê nhiều phòng cùng lúc)
    OTHER        // dịch vụ đặc biệt khác
}
