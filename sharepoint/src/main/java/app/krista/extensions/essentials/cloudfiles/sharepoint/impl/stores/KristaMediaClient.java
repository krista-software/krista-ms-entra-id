package app.krista.extensions.essentials.cloudfiles.sharepoint.impl.stores;

import app.krista.ksdk.files.FileHandle;
import app.krista.ksdk.files.FileRepository;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class KristaMediaClient {

    private static final String zipDir = "/tmp/";
    private final List<String> unSupportedFileFormats = Arrays.asList("html", "php5", "pht", "phtml", "shtml", "asa", "cer", "asax", "swf", "xap", "jsp", "exe", "js");
    @Inject
    private FileRepository fileRepository;

    public static void compressFile(String zipFilePath, String dirPathToZip) throws IOException {
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFilePath))) {
            File fileToZip = new File(dirPathToZip);
            FileInputStream fis = new FileInputStream(fileToZip);
            ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
            zipOut.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
            fis.close();
            zipOut.closeEntry();
        }
    }

    /*
        This method is used to download a file from krista's media server
        it will take krista's file object as input and returns java.io.File object
     */

    /*
        This method is used to upload a file into krista's media server
        it will take java.io.File object as input and returns krista's file object
     */
    public app.krista.model.base.File toKristaFile(File file) throws IOException {
        if (isUnsupportedFileFormat(file.getName())) {
            String zipFilePath = zipDir + file.getName().substring(0, file.getName().lastIndexOf(".")) + ".zip";
            compressFile(zipFilePath, file.getAbsolutePath());
            file = new File(zipFilePath);
        }
        try (final FileHandle fileHandle = fileRepository.createNewFileByName(file.getName())) {
            fileHandle.setContent(new FileInputStream(file));
            return fileHandle.getFile();
        }
    }

    public File toJavaFile(app.krista.model.base.File file) throws IOException {

        try (FileHandle fileHandle = fileRepository.getFile(file)) {
            InputStream content = fileHandle.getContent();
            final File input = new File(file.getFileName());
            return convertInputStreamToFile(content, input);
        }
    }

    private boolean isUnsupportedFileFormat(String fileName) {
        String fileExtension = getFileExtension(fileName);
        return unSupportedFileFormats.contains(fileExtension);
    }

    private File convertInputStreamToFile(InputStream inputStream, File input) throws IOException {
        try (OutputStream outputStream = new FileOutputStream(input)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        return input;
    }

    private String getFileExtension(String fileName) {
        if (fileName.contains(".")) {
            return fileName.substring((fileName.lastIndexOf(".") + 1));
        }
        throw new IllegalArgumentException("Unsupported file format");
    }
}
