package vn.edu.iuh.fit.utils;

public class FormatPhoneNumber {
    public static String formatPhoneNumberTo0(String phone) {
        if (phone == null) return null;
        phone = phone.replaceAll("\\D", "");
        if (phone.startsWith("84") && phone.length() == 11) {
            return "0" + phone.substring(2);
        }
        if (phone.startsWith("0") && phone.length() == 10) {
            return phone;
        }
        return phone;
    }

    public static String formatPhoneNumberTo84(String phone) {
        if (phone == null) return null;
        phone = phone.replaceAll("\\D", "");
        if (phone.startsWith("0") && phone.length() == 10) {
            return "84" + phone.substring(1);
        }
        if (phone.startsWith("84") && phone.length() == 11) {
            return phone;
        }
        return phone;
    }
}