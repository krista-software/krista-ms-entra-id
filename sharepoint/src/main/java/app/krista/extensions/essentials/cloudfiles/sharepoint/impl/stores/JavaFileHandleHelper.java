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

    private final FileRepository repository;

    @Inject
    public JavaFileHandleHelper(FileRepository repository) {
        this.repository = repository;
    }


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
    
}

