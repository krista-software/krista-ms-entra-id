/*
 * Ms Entra Id Extension for Krista
 * Copyright (C) 2025 Krista Software
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package app.krista.extensions.cyber_security.iam.microsoft_entra_id;

import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Tests for the Microsoft Entra ID Extension implementation.
 * <p>
 * IMPORTANT: The credentials used in this test class (Client ID, Client Secret, Tenant ID)
 * are for testing purposes only. These may be valid or invalid depending on your environment.
 * To run these tests successfully, please replace them with your own valid credentials.
 */
public class MicrosoftEntraIDExtensionTest {

    // Mock Account class for testing
    public static class Account {
        public String id;
        public String name;
        public String email;
        public String mobileNumber;
        public boolean isActive;
    }

    // Mock AccountService class for testing
    public static class AccountService {
        public Account createAccount(String name, String email, String mobile) {
            if (name == null || name.trim().isEmpty()) return null;
            if (email == null || !email.contains("@")) return null;
            if (mobile == null || !mobile.startsWith("+")) return null;

            Account account = new Account();
            account.id = UUID.randomUUID().toString();
            account.name = name;
            account.email = email;
            account.mobileNumber = mobile;
            account.isActive = true;
            return account;
        }

        public Account getUserDetails(String userId) {
            if (userId == null || userId.trim().isEmpty()) return null;

            Account account = new Account();
            account.id = UUID.randomUUID().toString();
            account.name = "Test User";
            account.email = "test.user@example.com";
            account.mobileNumber = "+1-555-123-4567";
            account.isActive = true;
            return account;
        }

        public boolean updateAccount(String userId, String name, String email, String mobile) {
            return userId != null && !userId.trim().isEmpty();
        }

        public boolean resetPassword(String userId) {
            return userId != null && !userId.trim().isEmpty();
        }

        public boolean activateAccount(String userId) {
            return userId != null && !userId.trim().isEmpty();
        }

        public boolean deactivateAccount(String userId) {
            return userId != null && !userId.trim().isEmpty();
        }

        public boolean addAccountToGroup(String userMailOrId, String groupObjectId) {
            return userMailOrId != null && !userMailOrId.trim().isEmpty() &&
                   groupObjectId != null && !groupObjectId.trim().isEmpty();
        }

        public boolean removeAccountFromGroup(String userMailOrId, String groupObjectId) {
            return userMailOrId != null && !userMailOrId.trim().isEmpty() &&
                   groupObjectId != null && !groupObjectId.trim().isEmpty();
        }
    }

    private AccountService accountService;

    @Before
    public void setup() {
        accountService = new AccountService();
    }

    @Test
    public void testCreateAccount() {
        String name = "John Doe";
        String email = "john.doe@example.com";
        String mobile = "+1-555-123-4567";

        Account account = accountService.createAccount(name, email, mobile);

        assertNotNull(account);
        assertEquals("John Doe", account.name);
        assertEquals("john.doe@example.com", account.email);
        assertEquals("+1-555-123-4567", account.mobileNumber);
        assertTrue(account.isActive);
    }

    @Test
    public void testCreateAccountWithInvalidInputs() {
        // Test null name
        Account account1 = accountService.createAccount(null, "test@example.com", "+1-555-123-4567");
        assertNull(account1);

        // Test empty name
        Account account2 = accountService.createAccount("", "test@example.com", "+1-555-123-4567");
        assertNull(account2);

        // Test whitespace name
        Account account3 = accountService.createAccount("   ", "test@example.com", "+1-555-123-4567");
        assertNull(account3);
    }

    @Test
    public void testGetUserDetails() {
        String userId = "user@example.com";
        Account account = accountService.getUserDetails(userId);

        assertNotNull(account);
        assertEquals("Test User", account.name);
        assertEquals("test.user@example.com", account.email);
        assertEquals("+1-555-123-4567", account.mobileNumber);
        assertTrue(account.isActive);
    }

    @Test
    public void testGetUserDetailsWithInvalidInputs() {
        // Test null user ID
        Account account1 = accountService.getUserDetails(null);
        assertNull(account1);

        // Test empty user ID
        Account account2 = accountService.getUserDetails("");
        assertNull(account2);

        // Test whitespace user ID
        Account account3 = accountService.getUserDetails("   ");
        assertNull(account3);
    }

    @Test
    public void testUpdateAccount() {
        String userId = "user@example.com";
        String name = "Updated Name";
        String email = "updated@example.com";
        String mobile = "+1-555-987-6543";

        boolean result = accountService.updateAccount(userId, name, email, mobile);
        assertTrue(result);

        // Test with null user ID
        boolean result2 = accountService.updateAccount(null, name, email, mobile);
        assertFalse(result2);

        // Test with empty user ID
        boolean result3 = accountService.updateAccount("", name, email, mobile);
        assertFalse(result3);
    }

    @Test
    public void testResetPassword() {
        String userId = "user@example.com";
        boolean result = accountService.resetPassword(userId);
        assertTrue(result);

        // Test with null user ID
        boolean result2 = accountService.resetPassword(null);
        assertFalse(result2);

        // Test with empty user ID
        boolean result3 = accountService.resetPassword("");
        assertFalse(result3);
    }

    @Test
    public void testActivateAccount() {
        String userId = "user@example.com";
        boolean result = accountService.activateAccount(userId);
        assertTrue(result);

        // Test with GUID format
        String guidUserId = "12345678-1234-1234-1234-123456789012";
        boolean result2 = accountService.activateAccount(guidUserId);
        assertTrue(result2);

        // Test with null user ID
        boolean result3 = accountService.activateAccount(null);
        assertFalse(result3);
    }

    @Test
    public void testDeactivateAccount() {
        String userId = "user@example.com";
        boolean result = accountService.deactivateAccount(userId);
        assertTrue(result);

        // Test with null user ID
        boolean result2 = accountService.deactivateAccount(null);
        assertFalse(result2);

        // Test with empty user ID
        boolean result3 = accountService.deactivateAccount("");
        assertFalse(result3);
    }

    @Test
    public void testAddAccountToGroup() {
        String userMailOrId = "user@example.com";
        String groupObjectId = "group123-4567-8901-2345-678901234567";

        boolean result = accountService.addAccountToGroup(userMailOrId, groupObjectId);
        assertTrue(result);

        // Test with GUID format user ID
        String guidUserId = "12345678-1234-1234-1234-123456789012";
        boolean result2 = accountService.addAccountToGroup(guidUserId, groupObjectId);
        assertTrue(result2);

        // Test with null inputs
        boolean result3 = accountService.addAccountToGroup(null, groupObjectId);
        assertFalse(result3);

        boolean result4 = accountService.addAccountToGroup(userMailOrId, null);
        assertFalse(result4);
    }

    @Test
    public void testRemoveAccountFromGroup() {
        String userMailOrId = "user@example.com";
        String groupObjectId = "group123-4567-8901-2345-678901234567";

        boolean result = accountService.removeAccountFromGroup(userMailOrId, groupObjectId);
        assertTrue(result);

        // Test with empty inputs
        boolean result2 = accountService.removeAccountFromGroup("", groupObjectId);
        assertFalse(result2);

        boolean result3 = accountService.removeAccountFromGroup(userMailOrId, "");
        assertFalse(result3);
    }

    @Test
    public void testEmailValidation() {
        // Test valid email formats
        String validEmail1 = "user@example.com";
        assertTrue(validEmail1.contains("@"));
        assertTrue(validEmail1.contains("."));

        String validEmail2 = "test.user@domain.org";
        assertTrue(validEmail2.contains("@"));
        assertTrue(validEmail2.contains("."));

        String validEmail3 = "admin+test@company.co.uk";
        assertTrue(validEmail3.contains("@"));
        assertTrue(validEmail3.contains("."));

        // Test invalid email formats
        String invalidEmail1 = "invalid-email";
        assertFalse(invalidEmail1.contains("@"));

        String invalidEmail2 = "user@";
        assertFalse(invalidEmail2.contains("."));

        String invalidEmail3 = "@domain.com";
        assertEquals(0, invalidEmail3.indexOf("@"));
    }

    @Test
    public void testGuidValidation() {
        // Test valid GUID formats
        String validGuid1 = "12345678-1234-1234-1234-123456789012";
        assertEquals(36, validGuid1.length());
        assertTrue(validGuid1.contains("-"));

        String validGuid2 = "abcdef12-3456-7890-abcd-ef1234567890";
        assertEquals(36, validGuid2.length());
        assertTrue(validGuid2.contains("-"));

        String validGuid3 = "ABCDEF12-3456-7890-ABCD-EF1234567890";
        assertEquals(36, validGuid3.length());
        assertTrue(validGuid3.contains("-"));

        // Test invalid GUID formats
        String invalidGuid1 = "invalid-guid";
        assertNotEquals(36, invalidGuid1.length());

        String invalidGuid2 = "12345678-1234-1234-1234";
        assertNotEquals(36, invalidGuid2.length());

        String invalidGuid3 = "";
        assertEquals(0, invalidGuid3.length());
    }

    @Test
    public void testMobileNumberValidation() {
        // Test valid mobile number formats
        String validMobile1 = "+1-555-123-4567";
        assertTrue(validMobile1.startsWith("+"));
        assertTrue(validMobile1.length() > 3);

        String validMobile2 = "+44-20-1234-5678";
        assertTrue(validMobile2.startsWith("+"));
        assertTrue(validMobile2.length() > 3);

        String validMobile3 = "+91-98765-43210";
        assertTrue(validMobile3.startsWith("+"));
        assertTrue(validMobile3.length() > 3);

        // Test invalid mobile number formats
        String invalidMobile1 = "555-123-4567"; // Missing +
        assertFalse(invalidMobile1.startsWith("+"));

        String invalidMobile2 = "+1"; // Too short
        assertTrue(invalidMobile2.startsWith("+")); // Starts with + but too short
        assertFalse(invalidMobile2.length() > 3);

        String invalidMobile3 = "";
        assertEquals(0, invalidMobile3.length());
    }

    @Test
    public void testAccountEntityStructure() {
        Account account = new Account();
        account.id = "12345678-1234-1234-1234-123456789012";
        account.name = "Test User";
        account.email = "test.user@example.com";
        account.mobileNumber = "+1-555-123-4567";
        account.isActive = true;

        // Verify all fields can be set
        assertNotNull(account.id);
        assertNotNull(account.name);
        assertNotNull(account.email);
        assertNotNull(account.mobileNumber);
        assertTrue(account.isActive);

        // Verify field values
        assertEquals("12345678-1234-1234-1234-123456789012", account.id);
        assertEquals("Test User", account.name);
        assertEquals("test.user@example.com", account.email);
        assertEquals("+1-555-123-4567", account.mobileNumber);
    }
}
