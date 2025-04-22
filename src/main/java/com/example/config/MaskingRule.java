package com.example.config;

import org.springframework.stereotype.Component;

@Component  // 添加这个注解使其成为 Spring Bean
public class MaskingRule {
    // 用户名前缀保留长度
    private int usernamePrefixLength = 1;
    // 用户名后缀保留长度
    private int usernameSuffixLength = 1;
    // 手机号前缀保留长度
    private int phonePrefixLength = 3;
    // 手机号后缀保留长度
    private int phoneSuffixLength = 4;
    // 邮箱前缀保留长度
    private int emailPrefixLength = 2;

    public int getUsernamePrefixLength() {
        return usernamePrefixLength;
    }

    public void setUsernamePrefixLength(int usernamePrefixLength) {
        if (usernamePrefixLength >= 0) {
            this.usernamePrefixLength = usernamePrefixLength;
        }
    }

    public int getUsernameSuffixLength() {
        return usernameSuffixLength;
    }

    public void setUsernameSuffixLength(int usernameSuffixLength) {
        if (usernameSuffixLength >= 0) {
            this.usernameSuffixLength = usernameSuffixLength;
        }
    }

    public int getPhonePrefixLength() {
        return phonePrefixLength;
    }

    public void setPhonePrefixLength(int phonePrefixLength) {
        if (phonePrefixLength >= 0) {
            this.phonePrefixLength = phonePrefixLength;
        }
    }

    public int getPhoneSuffixLength() {
        return phoneSuffixLength;
    }

    public void setPhoneSuffixLength(int phoneSuffixLength) {
        if (phoneSuffixLength >= 0) {
            this.phoneSuffixLength = phoneSuffixLength;
        }
    }

    public int getEmailPrefixLength() {
        return emailPrefixLength;
    }

    public void setEmailPrefixLength(int emailPrefixLength) {
        if (emailPrefixLength >= 0) {
            this.emailPrefixLength = emailPrefixLength;
        }
    }
}