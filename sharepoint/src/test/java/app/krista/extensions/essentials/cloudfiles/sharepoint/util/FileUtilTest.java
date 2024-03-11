package app.krista.extensions.essentials.cloudfiles.sharepoint.util;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class FileUtilTest {
    @Test
    public void testGetBaseNameAndExtension() {
        String fileName1 = "abced.eede.txt";
        String fileName2 = "filewithoutextension";
        String fileName3 = ".hiddenfile";
        String fileName4 = "file.with.multiple.extensions.jpeg";
        String fileName5 = "abcddvw.txt";
        String[][] testCases = {
                {fileName1, "abced.eede", "txt"},
                {fileName2, "filewithoutextension", ""},
                {fileName3, ".hiddenfile", ""},
                {fileName4, "file.with.multiple.extensions", "jpeg"},
                {fileName5, "abcddvw", "txt"}
        };
        for (String[] testCase : testCases) {
            List<String> result = FileUtil.getBaseName(testCase[0]);
            assertEquals(testCase[1], result.get(0)); // Base Name assertion
            assertEquals(testCase[2], result.get(1)); // Extension assertion
        }
    }

    @Test
    public void testGetlastPathElement() {
        String path1 = "/Enng/test";
        String path2 = "/folder1/folder2/file.txt";
        String path3 = "/onlyOneElement";

        assertEquals("test", FileUtil.getLastPathElement(path1));
        assertEquals("file.txt", FileUtil.getLastPathElement(path2));
        assertEquals("onlyOneElement", FileUtil.getLastPathElement(path3));
    }

    @Test
    public void testGetUsersWithPermission() {
        List<Map<String, Object>> share = new ArrayList<>();
        Map<String, Object> user1 = new HashMap<>();
        user1.put("User", "user1@example.com");
        user1.put("Permissions", "Read");
        share.add(user1);
        Map<String, Object> user2 = new HashMap<>();
        user2.put("User", "user2@example.com");
        user2.put("Permissions", "Write");
        share.add(user2);
        Map<String, Object> user3 = new HashMap<>();
        user3.put("User", "user3@example.com");
        user3.put("Permissions", "ReadAndWrite");
        share.add(user3);
        List<String> usersWithReadPermission = FileUtil.getUsersWithPermission(share, "Read");
        List<String> usersWithWritePermission = FileUtil.getUsersWithPermission(share, "Write");
        usersWithWritePermission.addAll(FileUtil.getUsersWithPermission(share, "ReadAndWrite"));
        assertEquals(1, usersWithReadPermission.size());
        assertEquals("user1@example.com", usersWithReadPermission.get(0));
        assertEquals(2, usersWithWritePermission.size());
        assertEquals("user2@example.com", usersWithWritePermission.get(0));
        assertEquals("user3@example.com", usersWithWritePermission.get(1));
    }

    @Test
    public void testReadFile() {
        String inputString = "This is a test string.\nWith multiple lines.\n";
        InputStream inputStream = new ByteArrayInputStream(inputString.getBytes(StandardCharsets.UTF_8));
        String result;
        try {
            result = FileUtil.readFile(inputStream);
        } catch (RuntimeException e) {
            throw new AssertionError("Unexpected exception: " + e.getMessage(), e);
        }
        assertEquals(inputString, result);
    }

    @Test
    public void testReadFileWithIOException() {
        InputStream inputStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Simulated IOException");
            }
        };
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            FileUtil.readFile(inputStream);
        });
        assertEquals(IOException.class, exception.getCause().getClass());
    }

    @Test
    public void testGetParentPath() {
        String relativePath = "Engg/SDoc/Test/ABC";
        String expectedParentPath = "Engg/SDoc/Test";
        String actualParentPath = FileUtil.getParentPath(relativePath);

        assertEquals(expectedParentPath, actualParentPath);
    }

    @Test
    public void testGetParentPathInvalidInput() {
        String relativePath = "Engg";
        assertNull(FileUtil.getParentPath(relativePath));
    }


    @Test
    public void testCreateTxtFile() {
        String fileName = "testFile.txt";
        String fileContent = "This is a test file content.";
        File createdFile = null;
        try {
            createdFile = FileUtil.createTxtFile(fileName, fileContent);
            assertTrue(createdFile.exists());
            assertTrue(createdFile.isFile());
        } catch (IOException e) {
            fail("IOException occurred: " + e.getMessage());
        } finally {
            if (createdFile != null && createdFile.exists()) {
                assertTrue(createdFile.delete());
            }
        }
    }

    @Test
    public void testExtractSecondLastFolderWithMultipleFolders() {
        String path = "https://contoso.sharepoint.com/folder1/folder%20with%20space";
        assertEquals("folder1", FileUtil.extractSecondLastFolder(path));
    }

    @Test
    public void testExtractSecondLastFolderWithSingleFolder() {
        String path = "https://contoso.sharepoint.com/Sample";
        assertEquals("", FileUtil.extractSecondLastFolder(path)); // There is only one folder
    }

    @Test
    public void testExtractSecondLastFolderWithEmptyPath() {
        String path = "";
        assertEquals("", FileUtil.extractSecondLastFolder(path)); // Empty path
    }

    @Test
    public void testExtractSecondLastFolderWithPathWithoutFolders() {
        String path = "https://contoso.sharepoint.com/";
        assertEquals("", FileUtil.extractSecondLastFolder(path)); // No folders
    }

    @Test
    public void testExtractSecondLastFolderWithPathContainingSpecialCharacters() {
        String path = "https://contoso.sharepoint.com/folder1/folder%20with%20space/folder3";
        assertEquals("folder%20with%20space", FileUtil.extractSecondLastFolder(path));
    }


    @Test
    public void testExtractSiteID_Success() {
        String input = "SPO:SPO_036f1078-1c4a-44bc-88b7-9e52dds6f789d@SPO_3694f6b4-b5f1-47ef-852f-a0b4a459ab44";
        String expectedSiteID = "036f1078-1c4a-44bc-88b7-9e52dds6f789d";

        String extractedSiteID = FileUtil.extractSiteID(input);

        assertEquals(expectedSiteID, extractedSiteID);
    }

    @Test
    public void testExtractSiteID_NoMatch() {
        String input = "Invalid string without site ID";
        String expectedSiteID = null;

        String extractedSiteID = FileUtil.extractSiteID(input);

        assertNull(extractedSiteID);
    }
}