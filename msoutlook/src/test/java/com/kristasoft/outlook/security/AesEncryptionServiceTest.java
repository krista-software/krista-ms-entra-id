package com.kristasoft.outlook.security;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for AesEncryptionService
 */
public class AesEncryptionServiceTest {

    private EncryptionService encryptionService;
    private String encryptionKey;

    @Before
    public void setUp() {
        // Generate a new encryption key for each test
        encryptionKey = AesEncryptionService.generateEncryptionKey();
        encryptionService = new AesEncryptionService(encryptionKey);
    }

    @Test
    public void testEncryptAndDecryptText() throws Exception {
        // Arrange
        String plainText = "This is a secret message that needs to be encrypted";
        
        // Act
        String encryptedText = encryptionService.encrypt(plainText);
        String decryptedText = encryptionService.decrypt(encryptedText);
        
        // Assert
        assertNotEquals("Encrypted text should be different from plain text", plainText, encryptedText);
        assertEquals("Decrypted text should match original plain text", plainText, decryptedText);
    }

    @Test
    public void testEncryptAndDecryptEmptyString() throws Exception {
        // Arrange
        String plainText = "";
        
        // Act
        String encryptedText = encryptionService.encrypt(plainText);
        String decryptedText = encryptionService.decrypt(encryptedText);
        
        // Assert
        assertNotNull("Encrypted text should not be null", encryptedText);
        assertEquals("Decrypted text should match original empty string", plainText, decryptedText);
    }

    @Test
    public void testHandleNullInput() throws Exception {
        // Act & Assert
        assertNull("Encrypting null should return null", encryptionService.encrypt(null));
        assertNull("Decrypting null should return null", encryptionService.decrypt(null));
    }

    @Test
    public void testGenerateValidEncryptionKey() {
        // Act
        String generatedKey = AesEncryptionService.generateEncryptionKey();
        
        // Assert
        assertNotNull("Generated key should not be null", generatedKey);
        assertTrue("Generated key should be valid", AesEncryptionService.isValidEncryptionKey(generatedKey));
    }

    @Test
    public void testValidateEncryptionKeyLength16() throws Exception {
        validateKeyLength(16);
    }
    
    @Test
    public void testValidateEncryptionKeyLength24() throws Exception {
        validateKeyLength(24);
    }
    
    @Test
    public void testValidateEncryptionKeyLength32() throws Exception {
        validateKeyLength(32);
    }
    
    private void validateKeyLength(int keyLength) {
        // Arrange
        byte[] keyBytes = new byte[keyLength];
        new java.security.SecureRandom().nextBytes(keyBytes);
        String encodedKey = java.util.Base64.getEncoder().encodeToString(keyBytes);
        
        // Act & Assert
        assertTrue("Key with length " + keyLength + " bytes should be valid", 
                AesEncryptionService.isValidEncryptionKey(encodedKey));
    }

    @Test
    public void testRejectInvalidEncryptionKey() {
        // Arrange
        byte[] keyBytes = new byte[10]; // Invalid length
        new java.security.SecureRandom().nextBytes(keyBytes);
        String encodedKey = java.util.Base64.getEncoder().encodeToString(keyBytes);
        
        // Act & Assert
        assertFalse("Key with invalid length should be rejected", 
                AesEncryptionService.isValidEncryptionKey(encodedKey));
    }

    @Test
    public void testRejectNonBase64EncryptionKey() {
        // Act & Assert
        assertFalse("Non-Base64 string should be rejected", 
                AesEncryptionService.isValidEncryptionKey("not-a-base64-string!"));
    }

    @Test
    public void testRejectNullEncryptionKey() {
        // Act & Assert
        assertFalse("Null key should be rejected", 
                AesEncryptionService.isValidEncryptionKey(null));
    }

    @Test(expected = Exception.class)
    public void testThrowExceptionForInvalidData() throws Exception {
        // Arrange
        String invalidEncryptedData = "ThisIsNotValidEncryptedData";
        
        // Act - should throw exception
        encryptionService.decrypt(invalidEncryptedData);
    }

    @Test
    public void testDifferentKeysProduceDifferentCiphertexts() throws Exception {
        // Arrange
        String plainText = "Secret message";
        String secondKey = AesEncryptionService.generateEncryptionKey();
        EncryptionService secondService = new AesEncryptionService(secondKey);
        
        // Act
        String firstEncryption = encryptionService.encrypt(plainText);
        String secondEncryption = secondService.encrypt(plainText);
        
        // Assert
        assertNotEquals("Different keys should produce different ciphertexts", 
                firstEncryption, secondEncryption);
    }

    @Test
    public void testSamePlaintextProducesDifferentCiphertexts() throws Exception {
        // Arrange
        String plainText = "Same plaintext";
        
        // Act
        String firstEncryption = encryptionService.encrypt(plainText);
        String secondEncryption = encryptionService.encrypt(plainText);
        
        // Assert
        assertNotEquals("Same plaintext should produce different ciphertexts due to random IV", 
                firstEncryption, secondEncryption);
    }
    
    @Test
    public void testLongTextEncryptionDecryption() throws Exception {
        // Arrange
        StringBuilder longTextBuilder = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longTextBuilder.append("This is a very long text that needs to be encrypted. ");
        }
        String longText = longTextBuilder.toString();
        
        // Act
        String encryptedText = encryptionService.encrypt(longText);
        String decryptedText = encryptionService.decrypt(encryptedText);
        
        // Assert
        assertEquals("Long text should be correctly decrypted", longText, decryptedText);
    }
    
    @Test
    public void testSpecialCharactersEncryptionDecryption() throws Exception {
        // Arrange
        String specialChars = "!@#$%^&*()_+{}|:\"<>?[];',./`~áéíóúñÁÉÍÓÚÑ";
        
        // Act
        String encryptedText = encryptionService.encrypt(specialChars);
        String decryptedText = encryptionService.decrypt(encryptedText);
        
        // Assert
        assertEquals("Special characters should be correctly decrypted", specialChars, decryptedText);
    }
}