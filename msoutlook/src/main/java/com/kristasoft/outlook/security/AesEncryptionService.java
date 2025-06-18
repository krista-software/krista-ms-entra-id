package com.kristasoft.outlook.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Logger;

public class AesEncryptionService implements EncryptionService {
    private static final Logger LOGGER = Logger.getLogger(AesEncryptionService.class.getName());
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    
    private final SecretKey secretKey;
    
    public AesEncryptionService(String encryptionKey) {
        // Convert the encryption key to a SecretKey
        byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }
    
    @Override
    public String encrypt(String plainText) throws Exception {
        if (plainText == null) {
            return null;
        }
        
        // Generate a random IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        
        // Initialize cipher for encryption
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
        
        // Encrypt the plaintext
        byte[] cipherText = cipher.doFinal(plainText.getBytes("UTF-8"));
        
        // Combine IV and ciphertext
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
        byteBuffer.put(iv);
        byteBuffer.put(cipherText);
        
        // Encode as Base64 string
        return Base64.getEncoder().encodeToString(byteBuffer.array());
    }
    
    @Override
    public String decrypt(String encryptedText) throws Exception {
        if (encryptedText == null) {
            return null;
        }
        
        // Decode from Base64
        byte[] cipherMessage = Base64.getDecoder().decode(encryptedText);
        
        // Extract IV
        ByteBuffer byteBuffer = ByteBuffer.wrap(cipherMessage);
        byte[] iv = new byte[GCM_IV_LENGTH];
        byteBuffer.get(iv);
        
        // Extract ciphertext
        byte[] cipherText = new byte[byteBuffer.remaining()];
        byteBuffer.get(cipherText);
        
        // Initialize cipher for decryption
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
        
        // Decrypt the ciphertext
        byte[] plainText = cipher.doFinal(cipherText);
        
        // Convert to string
        return new String(plainText, "UTF-8");
    }
    
    /**
     * Generates a secure random encryption key suitable for AES-256
     * @return Base64 encoded encryption key
     */
    public static String generateEncryptionKey() {
        byte[] key = new byte[32]; // 256 bits
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
    
    /**
     * Validates if the provided encryption key is valid for AES
     * @param encryptionKey Base64 encoded encryption key
     * @return true if valid, false otherwise
     */
    public static boolean isValidEncryptionKey(String encryptionKey) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
            return keyBytes.length == 16 || keyBytes.length == 24 || keyBytes.length == 32; // 128, 192, or 256 bits
        } catch (Exception e) {
            LOGGER.warning("Invalid encryption key format: " + e.getMessage());
            return false;
        }
    }
}