package vn.edu.iuh.fit.services.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import vn.edu.iuh.fit.services.SmsService;
import vn.edu.iuh.fit.utils.FormatPhoneNumber;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public
class SmsServiceImpl implements SmsService {
    private static final Logger logger = LoggerFactory.getLogger(SmsServiceImpl.class);
    private final SnsClient snsClient;

    @Value("${aws.region}")
    private String awsRegion;

    private final Map<String, OtpDetails> otpStorage = new ConcurrentHashMap<>();
    private final Map<String, Boolean> otpVerified = new ConcurrentHashMap<>();
    private static final int OTP_LENGTH = 6;
    private static final long OTP_EXPIRY_SECONDS = 300; // 5 minutes

    private static class OtpDetails {
        private final String otp;
        private final Instant expiryTime;

        OtpDetails(String otp, Instant expiryTime) {
            this.otp = otp;
            this.expiryTime = expiryTime;
        }

        boolean isValid(String inputOtp) {
            return otp.equals(inputOtp) && Instant.now().isBefore(expiryTime);
        }
    }

    @Override
    public String generateOtp() {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        logger.debug("Generated OTP: {}", otp);
        return otp.toString();
    }

    @Override
    public void sendOtp(String phoneNumber) {
        String formattedPhone = FormatPhoneNumber.formatPhoneNumberTo84(phoneNumber);
        String otp = generateOtp();
        Instant expiryTime = Instant.now().plusSeconds(OTP_EXPIRY_SECONDS);
        otpStorage.put(formattedPhone, new OtpDetails(otp, expiryTime));

        String message = "Your OTP for registration is: " + otp + ". It is valid for 5 minutes.";
        logger.info("Sending OTP {} to phone number {}", otp, formattedPhone);

        PublishRequest publishRequest = PublishRequest.builder()
                .phoneNumber(formattedPhone)
                .message(message)
                .build();

        try {
            PublishResponse response = snsClient.publish(publishRequest);
            logger.info("Message sent successfully with ID: {} for phone number {}", response.messageId(), formattedPhone);
        } catch (Exception e) {
            otpStorage.remove(formattedPhone);
            logger.error("Failed to send OTP to {}: {}", formattedPhone, e.getMessage(), e);
            throw new RuntimeException("Failed to send OTP to " + formattedPhone + ": " + e.getMessage());
        }
    }

    @Override
    public boolean verifyOtp(String phoneNumber, String otp) {
        String formattedPhone = FormatPhoneNumber.formatPhoneNumberTo84(phoneNumber);
        OtpDetails otpDetails = otpStorage.get(formattedPhone);

        if (otpDetails == null) {
            logger.warn("No OTP found for phone number: {}", formattedPhone);
            return false;
        }

        if (Instant.now().isAfter(otpDetails.expiryTime)) {
            logger.warn("OTP for phone number {} has expired", formattedPhone);
            otpStorage.remove(formattedPhone);
            return false;
        }

        boolean isValid = otpDetails.isValid(otp);
        logger.info("Verifying OTP for phone number {}: provided OTP {}, stored OTP {}, result: {}",
                formattedPhone, otp, otpDetails.otp, isValid);

        if (isValid) {
            otpStorage.remove(formattedPhone);
            logger.info("OTP verified successfully for phone number {}, OTP removed from storage", formattedPhone);
        }

        return isValid;
    }

    @Override
    public boolean isOtpVerified(String phoneNumber) {
        String formattedPhone = FormatPhoneNumber.formatPhoneNumberTo84(phoneNumber);
        return otpVerified.getOrDefault(formattedPhone, false);
    }

    @Override
    public void clearOtpVerification(String phoneNumber) {
        String formattedPhone = FormatPhoneNumber.formatPhoneNumberTo84(phoneNumber);
        otpVerified.remove(formattedPhone);
    }

    @Override
    public void setOtpVerified(String phone, boolean verified) {
        otpVerified.put(phone, verified);
    }
}