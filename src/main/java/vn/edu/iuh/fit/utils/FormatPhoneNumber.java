package vn.edu.iuh.fit.utils;

public class FormatPhoneNumber {
    public static String formatPhoneNumberTo0(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return null;
        }
        phoneNumber = phoneNumber.replaceAll("[^0-9]", ""); // Remove non-numeric characters
        if (phoneNumber.startsWith("84") && phoneNumber.length() > 2) {
            return "0" + phoneNumber.substring(2); // Convert +84987654300 to 0987654300
        }
        if (phoneNumber.startsWith("+84") && phoneNumber.length() > 3) {
            return "0" + phoneNumber.substring(3); // Convert +84987654300 to 0987654300
        }
        if (phoneNumber.startsWith("0")) {
            return phoneNumber; // Already in 0 format
        }
        return phoneNumber; // Return as is if format is unrecognized
    }

    public static String formatPhoneNumberTo84(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return null;
        }
        phoneNumber = phoneNumber.replaceAll("[^0-9]", ""); // Remove non-numeric characters
        if (phoneNumber.startsWith("0") && phoneNumber.length() > 1) {
            return "+84" + phoneNumber.substring(1); // Convert 0987654300 to +84987654300
        }
        if (phoneNumber.startsWith("84")) {
            return "+" + phoneNumber; // Convert 84987654300 to +84987654300
        }
        return "+84" + phoneNumber; // Default to +84 prefix
    }
}
