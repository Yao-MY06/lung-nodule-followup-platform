package com.yiliao.patient.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 身份证 AES-GCM 加解密（specs/modules/patient.md §4.3：id_card 加密存储、展示脱敏）。
 * 密钥来自配置（生产 Nacos/环境变量）；随机 IV 前置存储，格式 base64(iv + ciphertext)。
 */
@Component
public class IdCardCipher {

    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec keySpec;
    private final SecureRandom random = new SecureRandom();

    public IdCardCipher(@Value("${yiliao.patient.idcard-key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalStateException("yiliao.patient.idcard-key 必须为 32 字节 AES-256 密钥的 base64");
        }
        this.keySpec = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plain) {
        if (plain == null || plain.isBlank()) {
            return plain;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("身份证加密失败", e);
        }
    }

    public String decrypt(String stored) {
        if (stored == null || stored.isBlank()) {
            return stored;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(stored);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec,
                    new GCMParameterSpec(TAG_BITS, combined, 0, IV_LENGTH));
            byte[] plain = cipher.doFinal(combined, IV_LENGTH, combined.length - IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("身份证解密失败", e);
        }
    }

    /** 展示脱敏：保留前 3 后 4（specs/global/10 §9）。 */
    public static String mask(String plainIdCard) {
        if (plainIdCard == null || plainIdCard.length() < 8) {
            return "****";
        }
        return plainIdCard.substring(0, 3) + "****" + plainIdCard.substring(plainIdCard.length() - 4);
    }

    /** 手机号脱敏：138****5678。 */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone == null ? null : "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
