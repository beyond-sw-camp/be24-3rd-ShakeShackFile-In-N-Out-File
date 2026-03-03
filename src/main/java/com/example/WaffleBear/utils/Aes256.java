package com.example.WaffleBear.utils;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256/CBC 암호화 유틸리티
 * - 키는 환경 변수(AES_SECRET_KEY)에서 주입 (32자, 운영 환경 필수)
 * - 각 암호화마다 랜덤 IV(16 bytes) 생성 → ECB 패턴 노출 취약점 차단
 * - 암호문 구조: Base64(IV[16] + ciphertext)
 */
@Component
public class Aes256 {

    private static String SECRET_KEY;

    @Value("${project.aes.key}")
    public void setSecretKey(String key) {
        SECRET_KEY = key;
    }

    public static String encrypt(byte[] data) {
        try {
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);

            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(data);

            // IV + 암호문을 합쳐 Base64 인코딩
            byte[] ivAndEncrypted = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, ivAndEncrypted, 0, iv.length);
            System.arraycopy(encrypted, 0, ivAndEncrypted, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(ivAndEncrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encryption error", e);
        }
    }

    public static byte[] decrypt(byte[] encryptedData) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedData);

            // 앞 16 bytes = IV, 나머지 = 암호문
            byte[] iv = Arrays.copyOfRange(decoded, 0, 16);
            byte[] ciphertext = Arrays.copyOfRange(decoded, 16, decoded.length);

            SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new RuntimeException("Decryption error", e);
        }
    }
}
