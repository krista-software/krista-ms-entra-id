package app.krista.extensions.essentials.cloudfiles.sharepoint.util;

import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

public class DriveUtilTest {

    @Test
    public void testGetFileFromInputStream() {
        IllegalArgumentException exception = Assert.assertThrows(IllegalArgumentException.class, () -> DriveUtil.getFileFromInputStream(null, null));
        Assert.assertTrue(exception.getMessage().contains("Failed get file from input stream. Input stream is empty."));
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        exception = Assert.assertThrows(IllegalArgumentException.class, () -> DriveUtil.getFileFromInputStream(empty, null));
        Assert.assertTrue(exception.getMessage().contains("Failed get file from input stream. File name is empty."));
        exception = Assert.assertThrows(IllegalArgumentException.class, () -> DriveUtil.getFileFromInputStream(empty, ""));
        Assert.assertTrue(exception.getMessage().contains("Failed get file from input stream. File name is empty."));
        exception = Assert.assertThrows(IllegalArgumentException.class, () -> DriveUtil.getFileFromInputStream(empty, "  "));
        Assert.assertTrue(exception.getMessage().contains("Failed get file from input stream. File name is empty."));
    }

    @Test
    public void testStringValidator() {
        IllegalArgumentException exception = Assert.assertThrows(IllegalArgumentException.class, () -> DriveUtil.stringValidator(null, "Test string validator"));
        Assert.assertTrue(exception.getMessage().contains("Test string validator"));
        exception = Assert.assertThrows(IllegalArgumentException.class, () -> DriveUtil.stringValidator("", "Test string validator"));
        Assert.assertTrue(exception.getMessage().contains("Test string validator"));
        exception = Assert.assertThrows(IllegalArgumentException.class, () -> DriveUtil.stringValidator("  ", "Test string validator"));
        Assert.assertTrue(exception.getMessage().contains("Test string validator"));
    }

    @Test
    public void testListValidator() {
        IllegalArgumentException exception = Assert.assertThrows(IllegalArgumentException.class, () -> DriveUtil.listValidator(null, "Test list validator"));
        Assert.assertTrue(exception.getMessage().contains("Test list validator"));
        exception = Assert.assertThrows(IllegalArgumentException.class, () -> DriveUtil.listValidator(List.of(), "Test list validator"));
        Assert.assertTrue(exception.getMessage().contains("Test list validator"));
    }

    @Test
    public void testGetDriveItemForItem() {
        IllegalArgumentException exception = Assert.assertThrows(IllegalArgumentException.class, () -> DriveUtil.getDriveItemForItem(null));
        Assert.assertTrue(exception.getMessage().contains("Failed to get drive item. Folder name is empty."));
        exception = Assert.assertThrows(IllegalArgumentException.class, () -> DriveUtil.getDriveItemForItem(""));
        Assert.assertTrue(exception.getMessage().contains("Failed to get drive item. Folder name is empty."));
        exception = Assert.assertThrows(IllegalArgumentException.class, () -> DriveUtil.getDriveItemForItem("  "));
        Assert.assertTrue(exception.getMessage().contains("Failed to get drive item. Folder name is empty."));
    }

    @Test
    public void testGetRelativePath() {
        String fullUrl = "https://kristasoft.sharepoint.com/sites/Engineering/Shared%20Documents/TestAllFiles/Test Folder Two/Test Folder/Test.txt";
        String expectedRelativePath = "/TestAllFiles/Test Folder Two/Test Folder/Test.txt";
        String actualRelativePath = DriveUtil.getRelativePath(fullUrl);
        assertEquals(expectedRelativePath, actualRelativePath);

        fullUrl = "https://kristasoft.sharepoint.com/sites/Engineering/Shared%20Documents/Path%20with%20Spaces/Document.txt";
        expectedRelativePath = "/Path with Spaces/Document.txt";
        actualRelativePath = DriveUtil.getRelativePath(fullUrl);
        assertEquals(expectedRelativePath, actualRelativePath);

        fullUrl = "invalid-url";
        assertNull(DriveUtil.getRelativePath(fullUrl));

        fullUrl = "https://example.com/document.txt";
        assertNull(DriveUtil.getRelativePath(fullUrl));

        fullUrl = "/sites/Engineering/Shared%20Documents/Document.txt";
        expectedRelativePath = "/Document.txt";
        actualRelativePath = DriveUtil.getRelativePath(fullUrl);
        assertEquals(expectedRelativePath, actualRelativePath);
    }

    @Test
    public void testRemoveLastSegment() {
        String url = "https://kristasoft.sharepoint.com/sites/Engineering/Shared%20Documents/TestAllFiles/Test Folder Two/Test Folder/Test.txt";
        String expectedUrl = "https://kristasoft.sharepoint.com/sites/Engineering/Shared%20Documents/TestAllFiles/Test Folder Two/Test Folder";
        String actualUrl = DriveUtil.removeLastSegment(url);
        assertEquals(expectedUrl, actualUrl);

        url = "https://kristasoft.sharepoint.com/sites/Engineering/Shared%20Documents/Path%20with%20Spaces/Document.txt";
        expectedUrl = "https://kristasoft.sharepoint.com/sites/Engineering/Shared%20Documents/Path%20with%20Spaces";
        actualUrl = DriveUtil.removeLastSegment(url);
        assertEquals(expectedUrl, actualUrl);
    }

    @Test
    public void testGetRootUrl() {
        String filePath = "https://kristasoft.sharepoint.com/sites/Engineering/Shared%20Documents/TestAllFiles/Test Folder Two/Test Folder/Test.txt";
        String expectedRootUrl = "https://kristasoft.sharepoint.com/sites/Engineering/Shared%20Documents";
        String actualRootUrl = DriveUtil.getRootUrl(filePath);
        assertEquals(expectedRootUrl, actualRootUrl);

        filePath = "https://kristasoft.sharepoint.com/sites/Engineering/Shared%20Documents/Path%20with%20Spaces/Document.txt";
        expectedRootUrl = "https://kristasoft.sharepoint.com/sites/Engineering/Shared%20Documents";
        actualRootUrl = DriveUtil.getRootUrl(filePath);
        assertEquals(expectedRootUrl, actualRootUrl);

        filePath = "invalid-url";
        assertNull(DriveUtil.getRootUrl(filePath));

        filePath = "https://example.com";
        assertNull(DriveUtil.getRootUrl(filePath));

        filePath = "https://kristasoft.sharepoint.com/sites/Engineering/Shared%20Documents";
        assertNull(DriveUtil.getRootUrl(filePath));

        filePath = "/sites/Engineering/Shared%20Documents/Document.txt";
        assertNull(DriveUtil.getRootUrl(filePath));

        filePath = "";
        assertNull(DriveUtil.getRootUrl(filePath));

        filePath = "https://kristasoft.sharepoint.com/sites/Engineering/Shared%20Documents//";
        expectedRootUrl = "https://kristasoft.sharepoint.com/sites/Engineering/Shared%20Documents";
        actualRootUrl = DriveUtil.getRootUrl(filePath);
        assertEquals(expectedRootUrl, actualRootUrl);
    }

    @Test
    public void testRemoveFirstSlash() {
        String inputPathWithSlash = "/sites/Engineering/Shared%20Documents";
        String resultWithSlash = DriveUtil.removeFirstSlash(inputPathWithSlash);
        assertEquals("sites/Engineering/Shared%20Documents", resultWithSlash);

        String inputPathWithoutSlash = "sites/Engineering/Shared%20Documents";
        String resultWithoutSlash = DriveUtil.removeFirstSlash(inputPathWithoutSlash);
        assertEquals("sites/Engineering/Shared%20Documents", resultWithoutSlash);

        String inputPathEmpty = "";
        String resultEmpty = DriveUtil.removeFirstSlash(inputPathEmpty);
        assertEquals("", resultEmpty);

        String inputPathNull = null;
        String resultNull = DriveUtil.removeFirstSlash(inputPathNull);
        assertNull(resultNull);
    }

}