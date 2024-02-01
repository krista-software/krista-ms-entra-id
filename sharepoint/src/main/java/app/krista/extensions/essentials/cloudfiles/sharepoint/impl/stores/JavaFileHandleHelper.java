package app.krista.extensions.essentials.cloudfiles.sharepoint.impl.stores;

import app.krista.ksdk.files.FileHandle;
import app.krista.ksdk.files.FileRepository;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.*;

@Service
public class JavaFileHandleHelper {
    private static final Logger logger = LoggerFactory.getLogger(JavaFileHandleHelper.class);
    @Inject
    private FileRepository repository;


    public app.krista.model.base.File convertJavaFileToKristaFile(File file) throws IOException {

        try (final FileHandle outputFileHandle = this.repository.createNewFileByName(file.getName())) {
            try {
                outputFileHandle.setContent(new FileInputStream(file));
            } catch (IOException cause) {
                throw new RuntimeException("Failed to store content to file handle", cause);
            }
            return outputFileHandle.getFile();
        }
    }

    public File convertKristaFileToJavaFile(app.krista.model.base.File file) throws IOException {

        final FileHandle inputFileHandle = this.repository.getFile(file);

        final File tmpDir = new File(System.getProperty("java.io.tmpDir", "/tmp"));

        logger.info("Using tmp dir:{}", tmpDir.getAbsolutePath());

        if (tmpDir.exists() && !tmpDir.canWrite()) {
            logger.error("Unable to write to tmpDir: {}", tmpDir.getAbsolutePath());
            throw new IllegalArgumentException("Unable write to temporary file.");
        }

        final File input = new File(file.getFileName());
        return convertInputStreamToFile(inputFileHandle.getContent(), input);

    }

    public File convertInputStreamToFile(InputStream inputStream, File input) throws IOException {

        try (OutputStream outputStream = new FileOutputStream(input)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        return input;
    }
}

