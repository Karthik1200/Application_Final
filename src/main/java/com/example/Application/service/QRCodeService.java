package com.example.Application.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class QRCodeService {

    @Value("${app.qr.aes-key}")
    private String aesKey;

    /**
     * Encrypt data using AES-256
     */
    public String encrypt(String data) {
        try {
            byte[] key = Arrays.copyOf(
                    MessageDigest.getInstance("SHA-256").digest(aesKey.getBytes("UTF-8")),
                    32
            );
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes("UTF-8")));
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting QR data", e);
        }
    }

    /**
     * Decrypt AES-256 encrypted data
     */
    public String decrypt(String encryptedData) {
        try {
            byte[] key = Arrays.copyOf(
                    MessageDigest.getInstance("SHA-256").digest(aesKey.getBytes("UTF-8")),
                    32
            );
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedData)));
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting QR data", e);
        }
    }

    /**
     * Generate QR code image as Base64 string
     */
    public String generateQRCodeBase64(String data, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 2);

            BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height, hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (WriterException | java.io.IOException e) {
            throw new RuntimeException("Error generating QR code", e);
        }
    }

    /**
     * Create encrypted QR pass for visitor
     */
    public Map<String, String> createVisitorQRPass(Long visitorId, String visitorName, String purpose, long expiryTimestamp) {
        String qrData = String.format("VRGT|%d|%s|%s|%d", visitorId, visitorName, purpose, expiryTimestamp);
        String encryptedData = encrypt(qrData);
        String qrImageBase64 = generateQRCodeBase64(encryptedData, 300, 300);

        Map<String, String> result = new HashMap<>();
        result.put("encryptedData", encryptedData);
        result.put("qrImageBase64", qrImageBase64);
        return result;
    }

    /**
     * Validate QR pass
     */
    public Map<String, Object> validateQRPass(String encryptedData) {
        Map<String, Object> result = new HashMap<>();
        try {
            String decryptedData = decrypt(encryptedData);
            String[] parts = decryptedData.split("\\|");

            if (parts.length >= 4 && "VRGT".equals(parts[0])) {
                long expiryTimestamp = Long.parseLong(parts[3]);
                boolean expired = System.currentTimeMillis() > expiryTimestamp;

                result.put("valid", !expired);
                result.put("visitorId", Long.parseLong(parts[1]));
                result.put("visitorName", parts[2]);
                result.put("purpose", parts.length > 3 ? parts[2] : "");
                result.put("expired", expired);
            } else {
                result.put("valid", false);
                result.put("error", "Invalid QR format");
            }
        } catch (Exception e) {
            result.put("valid", false);
            result.put("error", "Invalid or tampered QR code");
        }
        return result;
    }
}
