package vn.edu.iuh.fit.services;

public interface SmsService {
    public String generateOtp();
    public void sendOtp(String phoneNumber);

    public boolean verifyOtp(String phoneNumber, String otp);
    boolean isOtpVerified(String phoneNumber);
    void clearOtpVerification(String phoneNumber);
    void setOtpVerified(String phone, boolean verified);
}
