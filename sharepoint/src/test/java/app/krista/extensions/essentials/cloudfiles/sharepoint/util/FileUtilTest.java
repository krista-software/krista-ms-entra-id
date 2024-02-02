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
    public void testGetParentPath() {
        String filePathWithShared = "shared/Teams/QA/03_Automation";
        String expectedPathWithShared = "/Teams/QA";
        assertEquals(expectedPathWithShared, FileUtil.getParentPath(filePathWithShared));

        String filePathWithSlashShared = "shared/Teams/QA/03_Automation/another_folder";
        String expectedPathWithSlashShared = "/Teams/QA/03_Automation";
        assertEquals(expectedPathWithSlashShared, FileUtil.getParentPath(filePathWithSlashShared));

        String filePathWithoutShared = "/drives/b!jxUeN0DgA02sUFccW1Pb1lXRHihDmDNCmAiRj77wj4mIw359lmkzQYFiwAJ5WZk3/root:/Teams/QA/03_Automation";
        String expectedPathWithoutShared = "/drives/b!jxUeN0DgA02sUFccW1Pb1lXRHihDmDNCmAiRj77wj4mIw359lmkzQYFiwAJ5WZk3/root:/Teams/QA";
        assertEquals(expectedPathWithoutShared, FileUtil.getParentPath(filePathWithoutShared));

        String pathWithoutSlash = "samplepath";
        assertThrows(IllegalArgumentException.class, () -> FileUtil.getParentPath(pathWithoutSlash));

        String emptyPath = "";
        assertThrows(IllegalArgumentException.class, () -> FileUtil.getParentPath(emptyPath));
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

}
