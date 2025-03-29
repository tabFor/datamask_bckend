package com.example.util;

public class DataMaskingUtil {

    // 脱敏用户名，只显示前两位和最后一位
    public static String maskUsername(String username) {
        if (username == null || username.length() < 3) {
            return username;
        }
        return username.substring(0, 2) + "****" + username.substring(username.length() - 1);
    }

    // 脱敏手机号，只显示前三位和后四位
    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 7) {
            return phoneNumber;
        }
        return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }

    // 脱敏邮箱，只显示前两位和@后面的域名
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        if (parts[0].length() < 3) {
            return email;
        }
        return parts[0].substring(0, 2) + "****@" + parts[1];
    }
}