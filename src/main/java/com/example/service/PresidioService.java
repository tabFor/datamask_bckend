package com.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 与Presidio服务通信的服务类
 */
@Service
public class PresidioService {

    private static final Logger logger = LoggerFactory.getLogger(PresidioService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${presidio.analyzer.url:http://localhost:5001}")
    private String analyzerUrl;

    @Value("${presidio.anonymizer.url:http://localhost:5002}")
    private String anonymizerUrl;

    public PresidioService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 使用Presidio分析文本中的敏感信息
     * 
     * @param text 待分析文本
     * @return 敏感信息列表
     */
    public List<Map<String, Object>> analyzeText(String text) {
        try {
            // 准备请求数据
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("text", text);
            requestBody.put("language", "en"); // 英文分析

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // 发送请求到Presidio分析器
            ResponseEntity<List> response = restTemplate.postForEntity(
                    analyzerUrl + "/analyze",
                    request,
                    List.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                logger.error("Presidio分析请求失败: {}", response.getStatusCode());
                return new ArrayList<>();
            }

            return response.getBody();
        } catch (Exception e) {
            logger.error("调用Presidio分析服务失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 添加个人身份识别器
     */
    private void addPersonalIdentifiers(List<Map<String, Object>> recognizers) {
        // 1.1 社会安全号码识别器 (SSN，美国)
        Map<String, Object> ssnRecognizer = new HashMap<>();
        ssnRecognizer.put("name", "US SSN Recognizer");
        ssnRecognizer.put("supported_language", "en");
        ssnRecognizer.put("supported_entity", "US_SSN");

        List<Map<String, Object>> ssnPatterns = new ArrayList<>();
        Map<String, Object> ssnPattern = new HashMap<>();
        ssnPattern.put("name", "US SSN Pattern");
        ssnPattern.put("regex", "\\b(?!000|666|9\\d{2})\\d{3}-(?!00)\\d{2}-(?!0000)\\d{4}\\b");
        ssnPattern.put("score", 0.85);
        ssnPatterns.add(ssnPattern);

        ssnRecognizer.put("patterns", ssnPatterns);
        ssnRecognizer.put("context", Arrays.asList("social", "security", "number", "ssn"));
        recognizers.add(ssnRecognizer);

        // 1.2 中国身份证号码识别器
        Map<String, Object> chinaIdRecognizer = new HashMap<>();
        chinaIdRecognizer.put("name", "Chinese ID Recognizer");
        chinaIdRecognizer.put("supported_language", "en");
        chinaIdRecognizer.put("supported_entity", "CHINA_ID");

        List<Map<String, Object>> chinaIdPatterns = new ArrayList<>();
        Map<String, Object> chinaIdPattern = new HashMap<>();
        chinaIdPattern.put("name", "Chinese ID Pattern");
        chinaIdPattern.put("regex",
                "\\b[1-9]\\d{5}(?:19|20)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\d{3}(?:\\d|X)\\b");
        chinaIdPattern.put("score", 0.85);
        chinaIdPatterns.add(chinaIdPattern);

        chinaIdRecognizer.put("patterns", chinaIdPatterns);
        chinaIdRecognizer.put("context", Arrays.asList("id", "identity", "chinese", "身份证"));
        recognizers.add(chinaIdRecognizer);

        // 1.3 护照号码识别器
        Map<String, Object> passportRecognizer = new HashMap<>();
        passportRecognizer.put("name", "Passport Recognizer");
        passportRecognizer.put("supported_language", "en");
        passportRecognizer.put("supported_entity", "PASSPORT");

        List<Map<String, Object>> passportPatterns = new ArrayList<>();
        // 中国护照
        Map<String, Object> chinaPassportPattern = new HashMap<>();
        chinaPassportPattern.put("name", "China Passport Pattern");
        chinaPassportPattern.put("regex", "\\b[GEDA]\\d{8}\\b");
        chinaPassportPattern.put("score", 0.8);
        passportPatterns.add(chinaPassportPattern);

        // 美国护照
        Map<String, Object> usPassportPattern = new HashMap<>();
        usPassportPattern.put("name", "US Passport Pattern");
        usPassportPattern.put("regex", "\\b[A-Z]\\d{8}\\b");
        usPassportPattern.put("score", 0.8);
        passportPatterns.add(usPassportPattern);

        // 通用护照格式
        Map<String, Object> generalPassportPattern = new HashMap<>();
        generalPassportPattern.put("name", "General Passport Pattern");
        generalPassportPattern.put("regex", "\\b[A-Z]{1,2}[0-9]{6,9}\\b");
        generalPassportPattern.put("score", 0.7);
        passportPatterns.add(generalPassportPattern);

        passportRecognizer.put("patterns", passportPatterns);
        passportRecognizer.put("context", Arrays.asList("passport", "travel", "document", "护照"));
        recognizers.add(passportRecognizer);

        // 1.4 驾驶证号码识别器
        Map<String, Object> driverLicenseRecognizer = new HashMap<>();
        driverLicenseRecognizer.put("name", "Driver License Recognizer");
        driverLicenseRecognizer.put("supported_language", "en");
        driverLicenseRecognizer.put("supported_entity", "DRIVER_LICENSE");

        List<Map<String, Object>> driverLicensePatterns = new ArrayList<>();
        // 中国驾驶证
        Map<String, Object> chinaDriverLicensePattern = new HashMap<>();
        chinaDriverLicensePattern.put("name", "China Driver License Pattern");
        chinaDriverLicensePattern.put("regex", "\\b[1-9]\\d{11}[0-9A-Z]{1}\\b");
        chinaDriverLicensePattern.put("score", 0.8);
        driverLicensePatterns.add(chinaDriverLicensePattern);

        driverLicenseRecognizer.put("patterns", driverLicensePatterns);
        driverLicenseRecognizer.put("context", Arrays.asList("driver", "license", "driving", "驾驶证"));
        recognizers.add(driverLicenseRecognizer);
    }

    /**
     * 添加联系信息识别器
     */
    private void addContactInformation(List<Map<String, Object>> recognizers) {
        // 2.1 电话号码识别器
        Map<String, Object> phoneRecognizer = new HashMap<>();
        phoneRecognizer.put("name", "Phone Number Recognizer");
        phoneRecognizer.put("supported_language", "en");
        phoneRecognizer.put("supported_entity", "PHONE_NUMBER");

        List<Map<String, Object>> phonePatterns = new ArrayList<>();
        // 中国手机号
        Map<String, Object> chinaPhonePattern = new HashMap<>();
        chinaPhonePattern.put("name", "China Phone Pattern");
        chinaPhonePattern.put("regex", "\\b1[3-9]\\d{9}\\b");
        chinaPhonePattern.put("score", 0.75);
        phonePatterns.add(chinaPhonePattern);

        // 中国座机号
        Map<String, Object> chinaLandlinePattern = new HashMap<>();
        chinaLandlinePattern.put("name", "China Landline Pattern");
        chinaLandlinePattern.put("regex", "\\b(?:0[0-9]{2,3}[-]?)?[2-9][0-9]{6,7}(?:[-][0-9]{1,4})?\\b");
        chinaLandlinePattern.put("score", 0.75);
        phonePatterns.add(chinaLandlinePattern);

        // 美国电话号码
        Map<String, Object> usPhonePattern = new HashMap<>();
        usPhonePattern.put("name", "US Phone Pattern");
        usPhonePattern.put("regex", "\\b(?:\\+?1[-\\s]?)?(?:\\([0-9]{3}\\)|[0-9]{3})[-\\s]?[0-9]{3}[-\\s]?[0-9]{4}\\b");
        usPhonePattern.put("score", 0.75);
        phonePatterns.add(usPhonePattern);

        // 国际电话号码 (一般格式，以+开头)
        Map<String, Object> intlPhonePattern = new HashMap<>();
        intlPhonePattern.put("name", "International Phone Pattern");
        intlPhonePattern.put("regex", "\\b\\+[1-9][0-9]{1,2}[-\\s]?[0-9]{1,12}(?:[-\\s]?[0-9]{1,12})*\\b");
        intlPhonePattern.put("score", 0.7);
        phonePatterns.add(intlPhonePattern);

        phoneRecognizer.put("patterns", phonePatterns);
        phoneRecognizer.put("context", Arrays.asList("phone", "mobile", "telephone", "cell", "联系电话", "手机"));
        recognizers.add(phoneRecognizer);

        // 2.2 邮箱地址识别器
        Map<String, Object> emailRecognizer = new HashMap<>();
        emailRecognizer.put("name", "Email Address Recognizer");
        emailRecognizer.put("supported_language", "en");
        emailRecognizer.put("supported_entity", "EMAIL_ADDRESS");

        List<Map<String, Object>> emailPatterns = new ArrayList<>();
        Map<String, Object> emailPattern = new HashMap<>();
        emailPattern.put("name", "Email Pattern");
        emailPattern.put("regex", "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
        emailPattern.put("score", 0.85);
        emailPatterns.add(emailPattern);

        emailRecognizer.put("patterns", emailPatterns);
        emailRecognizer.put("context", Arrays.asList("email", "mail", "contact", "邮箱"));
        recognizers.add(emailRecognizer);

        // 2.3 IP地址识别器
        Map<String, Object> ipAddressRecognizer = new HashMap<>();
        ipAddressRecognizer.put("name", "IP Address Recognizer");
        ipAddressRecognizer.put("supported_language", "en");
        ipAddressRecognizer.put("supported_entity", "IP_ADDRESS");

        List<Map<String, Object>> ipPatterns = new ArrayList<>();

        // IPv4
        Map<String, Object> ipv4Pattern = new HashMap<>();
        ipv4Pattern.put("name", "IPv4 Pattern");
        ipv4Pattern.put("regex",
                "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b");
        ipv4Pattern.put("score", 0.75);
        ipPatterns.add(ipv4Pattern);

        // IPv6
        Map<String, Object> ipv6Pattern = new HashMap<>();
        ipv6Pattern.put("name", "IPv6 Pattern");
        ipv6Pattern.put("regex", "\\b(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}\\b");
        ipv6Pattern.put("score", 0.75);
        ipPatterns.add(ipv6Pattern);

        ipAddressRecognizer.put("patterns", ipPatterns);
        ipAddressRecognizer.put("context", Arrays.asList("ip", "address", "network"));
        recognizers.add(ipAddressRecognizer);
    }

    /**
     * 添加金融信息识别器
     */
    private void addFinancialInformation(List<Map<String, Object>> recognizers) {
        // 3.1 信用卡号码识别器
        Map<String, Object> creditCardRecognizer = new HashMap<>();
        creditCardRecognizer.put("name", "Credit Card Recognizer");
        creditCardRecognizer.put("supported_language", "en");
        creditCardRecognizer.put("supported_entity", "CREDIT_CARD");

        List<Map<String, Object>> creditCardPatterns = new ArrayList<>();

        // Visa
        Map<String, Object> visaPattern = new HashMap<>();
        visaPattern.put("name", "Visa Card Pattern");
        visaPattern.put("regex", "\\b4[0-9]{12}(?:[0-9]{3})?\\b");
        visaPattern.put("score", 0.8);
        creditCardPatterns.add(visaPattern);

        // MasterCard
        Map<String, Object> masterCardPattern = new HashMap<>();
        masterCardPattern.put("name", "MasterCard Pattern");
        masterCardPattern.put("regex", "\\b5[1-5][0-9]{14}\\b");
        masterCardPattern.put("score", 0.8);
        creditCardPatterns.add(masterCardPattern);

        // American Express
        Map<String, Object> amexPattern = new HashMap<>();
        amexPattern.put("name", "Amex Card Pattern");
        amexPattern.put("regex", "\\b3[47][0-9]{13}\\b");
        amexPattern.put("score", 0.8);
        creditCardPatterns.add(amexPattern);

        // 通用信用卡格式 (包括带间隔的格式)
        Map<String, Object> genericCardPattern = new HashMap<>();
        genericCardPattern.put("name", "Generic Card Pattern");
        genericCardPattern.put("regex", "\\b(?:\\d[ -]*?){13,16}\\b");
        genericCardPattern.put("score", 0.6);
        creditCardPatterns.add(genericCardPattern);

        creditCardRecognizer.put("patterns", creditCardPatterns);
        creditCardRecognizer.put("context", Arrays.asList("credit", "card", "visa", "mastercard", "amex", "信用卡"));
        recognizers.add(creditCardRecognizer);

        // 3.2 中国银行卡号识别器
        Map<String, Object> chinaBankCardRecognizer = new HashMap<>();
        chinaBankCardRecognizer.put("name", "China Bank Card Recognizer");
        chinaBankCardRecognizer.put("supported_language", "en");
        chinaBankCardRecognizer.put("supported_entity", "BANK_CARD");

        List<Map<String, Object>> bankCardPatterns = new ArrayList<>();
        Map<String, Object> bankCardPattern = new HashMap<>();
        bankCardPattern.put("name", "China Bank Card Pattern");
        bankCardPattern.put("regex", "\\b(62|45|60)\\d{14,18}\\b");
        bankCardPattern.put("score", 0.8);
        bankCardPatterns.add(bankCardPattern);

        chinaBankCardRecognizer.put("patterns", bankCardPatterns);
        chinaBankCardRecognizer.put("context", Arrays.asList("bank", "card", "account", "银行卡"));
        recognizers.add(chinaBankCardRecognizer);

        // 3.3 IBAN 国际银行账号识别器
        Map<String, Object> ibanRecognizer = new HashMap<>();
        ibanRecognizer.put("name", "IBAN Recognizer");
        ibanRecognizer.put("supported_language", "en");
        ibanRecognizer.put("supported_entity", "IBAN");

        List<Map<String, Object>> ibanPatterns = new ArrayList<>();
        Map<String, Object> ibanPattern = new HashMap<>();
        ibanPattern.put("name", "IBAN Pattern");
        ibanPattern.put("regex", "\\b[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}\\b");
        ibanPattern.put("score", 0.8);
        ibanPatterns.add(ibanPattern);

        ibanRecognizer.put("patterns", ibanPatterns);
        ibanRecognizer.put("context", Arrays.asList("iban", "international", "bank", "account"));
        recognizers.add(ibanRecognizer);

        // 3.4 比特币地址识别器
        Map<String, Object> bitcoinRecognizer = new HashMap<>();
        bitcoinRecognizer.put("name", "Bitcoin Address Recognizer");
        bitcoinRecognizer.put("supported_language", "en");
        bitcoinRecognizer.put("supported_entity", "CRYPTO");

        List<Map<String, Object>> bitcoinPatterns = new ArrayList<>();
        Map<String, Object> bitcoinPattern = new HashMap<>();
        bitcoinPattern.put("name", "Bitcoin Address Pattern");
        bitcoinPattern.put("regex", "\\b(bc1|[13])[a-zA-HJ-NP-Z0-9]{25,39}\\b");
        bitcoinPattern.put("score", 0.75);
        bitcoinPatterns.add(bitcoinPattern);

        bitcoinRecognizer.put("patterns", bitcoinPatterns);
        bitcoinRecognizer.put("context", Arrays.asList("bitcoin", "btc", "crypto", "wallet", "比特币"));
        recognizers.add(bitcoinRecognizer);
    }

    /**
     * 添加医疗信息识别器
     */
    private void addHealthInformation(List<Map<String, Object>> recognizers) {
        // 4.1 医疗保险号码识别器
        Map<String, Object> medicalIdRecognizer = new HashMap<>();
        medicalIdRecognizer.put("name", "Medical ID Recognizer");
        medicalIdRecognizer.put("supported_language", "en");
        medicalIdRecognizer.put("supported_entity", "MEDICAL_ID");

        List<Map<String, Object>> medicalIdPatterns = new ArrayList<>();

        // 美国医疗保险号码 (Medicare)
        Map<String, Object> medicarePattern = new HashMap<>();
        medicarePattern.put("name", "Medicare Number Pattern");
        medicarePattern.put("regex", "\\b[1-9][0-9]{2}-[0-9]{2}-[0-9]{4}[A-Z]\\b");
        medicarePattern.put("score", 0.8);
        medicalIdPatterns.add(medicarePattern);

        medicalIdRecognizer.put("patterns", medicalIdPatterns);
        medicalIdRecognizer.put("context", Arrays.asList("medical", "health", "insurance", "medicare", "医保"));
        recognizers.add(medicalIdRecognizer);
    }

    /**
     * 添加位置信息识别器
     */
    private void addLocationInformation(List<Map<String, Object>> recognizers) {
        // 5.1 邮政编码识别器
        Map<String, Object> postalCodeRecognizer = new HashMap<>();
        postalCodeRecognizer.put("name", "Postal Code Recognizer");
        postalCodeRecognizer.put("supported_language", "en");
        postalCodeRecognizer.put("supported_entity", "POSTAL_CODE");

        List<Map<String, Object>> postalCodePatterns = new ArrayList<>();

        // 中国邮编
        Map<String, Object> chinaPostalPattern = new HashMap<>();
        chinaPostalPattern.put("name", "China Postal Code Pattern");
        chinaPostalPattern.put("regex", "\\b[1-9][0-9]{5}\\b");
        chinaPostalPattern.put("score", 0.7);
        postalCodePatterns.add(chinaPostalPattern);

        // 美国邮编
        Map<String, Object> usPostalPattern = new HashMap<>();
        usPostalPattern.put("name", "US Postal Code Pattern");
        usPostalPattern.put("regex", "\\b[0-9]{5}(?:-[0-9]{4})?\\b");
        usPostalPattern.put("score", 0.7);
        postalCodePatterns.add(usPostalPattern);

        postalCodeRecognizer.put("patterns", postalCodePatterns);
        postalCodeRecognizer.put("context", Arrays.asList("postal", "code", "zip", "邮编", "邮政编码"));
        recognizers.add(postalCodeRecognizer);

        // 5.2 经纬度识别器
        Map<String, Object> coordinatesRecognizer = new HashMap<>();
        coordinatesRecognizer.put("name", "Coordinates Recognizer");
        coordinatesRecognizer.put("supported_language", "en");
        coordinatesRecognizer.put("supported_entity", "COORDINATES");

        List<Map<String, Object>> coordinatesPatterns = new ArrayList<>();
        Map<String, Object> coordinatesPattern = new HashMap<>();
        coordinatesPattern.put("name", "Coordinates Pattern");
        coordinatesPattern.put("regex",
                "\\b[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?),\\s*[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)\\b");
        coordinatesPattern.put("score", 0.7);
        coordinatesPatterns.add(coordinatesPattern);

        coordinatesRecognizer.put("patterns", coordinatesPatterns);
        coordinatesRecognizer.put("context",
                Arrays.asList("coordinates", "latitude", "longitude", "gps", "position", "经纬度"));
        recognizers.add(coordinatesRecognizer);
    }

    /**
     * 添加账号和密码相关识别器
     */
    private void addAccountInformation(List<Map<String, Object>> recognizers) {
        // 6.1 API密钥识别器
        Map<String, Object> apiKeyRecognizer = new HashMap<>();
        apiKeyRecognizer.put("name", "API Key Recognizer");
        apiKeyRecognizer.put("supported_language", "en");
        apiKeyRecognizer.put("supported_entity", "API_KEY");

        List<Map<String, Object>> apiKeyPatterns = new ArrayList<>();

        // 通用API密钥格式
        Map<String, Object> genericApiKeyPattern = new HashMap<>();
        genericApiKeyPattern.put("name", "Generic API Key Pattern");
        genericApiKeyPattern.put("regex", "\\b[A-Za-z0-9_-]{20,64}\\b");
        genericApiKeyPattern.put("score", 0.5); // 设置较低的分数，因为可能会有误报
        apiKeyPatterns.add(genericApiKeyPattern);

        apiKeyRecognizer.put("patterns", apiKeyPatterns);
        apiKeyRecognizer.put("context", Arrays.asList("api", "key", "secret", "token"));
        recognizers.add(apiKeyRecognizer);

        // 6.2 密码识别器
        Map<String, Object> passwordRecognizer = new HashMap<>();
        passwordRecognizer.put("name", "Password Recognizer");
        passwordRecognizer.put("supported_language", "en");
        passwordRecognizer.put("supported_entity", "PASSWORD");

        List<Map<String, Object>> passwordPatterns = new ArrayList<>();
        Map<String, Object> passwordPattern = new HashMap<>();
        passwordPattern.put("name", "Password Pattern");
        passwordPattern.put("regex", "(?<=password[:|=|\\s])\\S{8,}");
        passwordPattern.put("score", 0.7);
        passwordPatterns.add(passwordPattern);

        passwordRecognizer.put("patterns", passwordPatterns);
        passwordRecognizer.put("context", Arrays.asList("password", "pwd", "pass", "密码"));
        recognizers.add(passwordRecognizer);
    }

    /**
     * 添加职业和教育信息识别器
     */
    private void addProfessionalInformation(List<Map<String, Object>> recognizers) {
        // 7.1 学生证号码识别器
        Map<String, Object> studentIdRecognizer = new HashMap<>();
        studentIdRecognizer.put("name", "Student ID Recognizer");
        studentIdRecognizer.put("supported_language", "en");
        studentIdRecognizer.put("supported_entity", "STUDENT_ID");

        List<Map<String, Object>> studentIdPatterns = new ArrayList<>();
        Map<String, Object> studentIdPattern = new HashMap<>();
        studentIdPattern.put("name", "Student ID Pattern");
        studentIdPattern.put("regex", "\\b[0-9]{8,12}\\b");
        studentIdPattern.put("score", 0.5); // 较低分数，因为可能会误报
        studentIdPatterns.add(studentIdPattern);

        studentIdRecognizer.put("patterns", studentIdPatterns);
        studentIdRecognizer.put("context",
                Arrays.asList("student", "ID", "college", "university", "school", "学号", "学生"));
        recognizers.add(studentIdRecognizer);
    }

    /**
     * 使用Presidio对文本进行匿名化处理
     * 
     * @param text            原始文本
     * @param analyzerResults 分析结果
     * @return 匿名化后的文本
     */
    public String anonymizeText(String text, List<Map<String, Object>> analyzerResults) {
        try {
            // 准备请求数据
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("text", text);
            requestBody.put("analyzer_results", analyzerResults);

            // 设置默认的匿名化操作器
            Map<String, Object> anonymizers = new HashMap<>();

            // 默认替换为星号
            Map<String, Object> defaultAnonymizer = new HashMap<>();
            defaultAnonymizer.put("type", "replace");
            defaultAnonymizer.put("new_value", "****");
            anonymizers.put("DEFAULT", defaultAnonymizer);

            // 身份证号匿名化
            Map<String, Object> idcardAnonymizer = new HashMap<>();
            idcardAnonymizer.put("type", "mask");
            idcardAnonymizer.put("masking_char", "*");
            idcardAnonymizer.put("chars_to_mask", 10);
            idcardAnonymizer.put("from_end", false);
            anonymizers.put("PERSON", idcardAnonymizer);
            anonymizers.put("ID_NUMBER", idcardAnonymizer);

            // 邮箱匿名化
            Map<String, Object> emailAnonymizer = new HashMap<>();
            emailAnonymizer.put("type", "mask");
            emailAnonymizer.put("masking_char", "*");
            emailAnonymizer.put("chars_to_mask", 5);
            emailAnonymizer.put("from_end", false);
            anonymizers.put("EMAIL_ADDRESS", emailAnonymizer);

            // 电话号码匿名化
            Map<String, Object> phoneAnonymizer = new HashMap<>();
            phoneAnonymizer.put("type", "mask");
            phoneAnonymizer.put("masking_char", "*");
            phoneAnonymizer.put("chars_to_mask", 7);
            phoneAnonymizer.put("from_end", false);
            anonymizers.put("PHONE_NUMBER", phoneAnonymizer);

            requestBody.put("anonymizers", anonymizers);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // 发送请求到Presidio匿名器
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    anonymizerUrl + "/anonymize",
                    request,
                    Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                logger.error("Presidio匿名化请求失败: {}", response.getStatusCode());
                return text;
            }

            return (String) response.getBody().get("text");
        } catch (Exception e) {
            logger.error("调用Presidio匿名化服务失败", e);
            return text;
        }
    }

    /**
     * 一步执行分析和匿名化
     * 
     * @param text 待处理文本
     * @return 匿名化后的文本
     */
    public String processText(String text) {
        List<Map<String, Object>> analyzerResults = analyzeText(text);
        if (analyzerResults.isEmpty()) {
            return text;
        }
        return anonymizeText(text, analyzerResults);
    }
}