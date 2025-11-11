package vn.edu.iuh.fit.services.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import vn.edu.iuh.fit.services.SmsService;
import vn.edu.iuh.fit.utils.FormatPhoneNumber;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {
    private static final Logger logger = LoggerFactory.getLogger(SmsServiceImpl.class);
    private final SnsClient snsClient;

    private final Map<String, OtpDetails> otpStorage = new ConcurrentHashMap<>();
    private final Map<String, Boolean> otpVerified = new ConcurrentHashMap<>();
    private final Map<String, Long> rateLimit = new ConcurrentHashMap<>();

    private static final int OTP_LENGTH = 6;
    private static final long OTP_EXPIRY_SECONDS = 300; // 5 phút
    private static final long RATE_LIMIT_MS = 60_000;   // 1 phút

    private static class OtpDetails {
        final String otp;
        final Instant expiryTime;

        OtpDetails(String otp, Instant expiryTime) {
            this.otp = otp;
            this.expiryTime = expiryTime;
        }
    }

    @Override
    public String generateOtp() {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    @Override
    public void sendOtp(String phoneNumber) {
        String formatted = FormatPhoneNumber.formatPhoneNumberTo84(phoneNumber);
        if (formatted.startsWith("84")) formatted = "+" + formatted;

        // Rate limit
        long now = System.currentTimeMillis();
        Long last = rateLimit.get(formatted);
        if (last != null && now - last < RATE_LIMIT_MS) {
            throw new RuntimeException("Vui lòng đợi 1 phút trước khi gửi lại OTP.");
        }
        rateLimit.put(formatted, now);

        String otp = generateOtp();
        Instant expiry = Instant.now().plusSeconds(OTP_EXPIRY_SECONDS);
        otpStorage.put(formatted, new OtpDetails(otp, expiry));

        logger.info("OTP cho {}: {} (hết hạn sau 5 phút)", formatted, otp);
        String message = "Mã OTP ZenRoom: " + otp + ". Hiệu lực 5 phút. Không chia sẻ!";

        Map<String, MessageAttributeValue> attrs = new HashMap<>();
        attrs.put("AWS.SNS.SMS.SMSType", MessageAttributeValue.builder()
                .dataType("String")
                .stringValue("Transactional")
                .build());

        // Bật sau khi AWS duyệt
        // attrs.put("AWS.SNS.SMS.SenderID", MessageAttributeValue.builder()
        //         .dataType("String")
        //         .stringValue("ZenRoom")
        //         .build());

        PublishRequest request = PublishRequest.builder()
                .message(message)
                .phoneNumber(formatted)
                .messageAttributes(attrs)
                .build();

        try {
            PublishResponse res = snsClient.publish(request);
            logger.info("SMS sent! ID: {}", res.messageId());
        } catch (Exception e) {
            otpStorage.remove(formatted);
            rateLimit.remove(formatted);
            logger.error("SMS failed: {}", e.getMessage());
            throw new RuntimeException("Gửi OTP thất bại: " + e.getMessage());
        }
    }

    @Override
    public boolean verifyOtp(String phoneNumber, String otp) {
        String formatted = FormatPhoneNumber.formatPhoneNumberTo84(phoneNumber);
        if (formatted.startsWith("84")) formatted = "+" + formatted;

        OtpDetails details = otpStorage.get(formatted);
        if (details == null) return false;

        if (Instant.now().isAfter(details.expiryTime)) {
            otpStorage.remove(formatted);
            return false;
        }

        if (details.otp.equals(otp)) {
            otpStorage.remove(formatted);
            return true;
        }
        return false;
    }

    @Override
    public boolean isOtpVerified(String phone) {
        return otpVerified.getOrDefault(FormatPhoneNumber.formatPhoneNumberTo0(phone), false);
    }

    @Override
    public void setOtpVerified(String phone, boolean verified) {
        otpVerified.put(FormatPhoneNumber.formatPhoneNumberTo0(phone), verified);
    }

    @Override
    public void clearOtpVerification(String phone) {
        otpVerified.remove(FormatPhoneNumber.formatPhoneNumberTo0(phone));
    }

}