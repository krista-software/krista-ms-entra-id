package app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores;

import app.krista.extension.executor.SearchCondition;
import app.krista.extension.executor.SearchQuery;
import app.krista.extension.util.EntityStore;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.CatalogTypes;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.ManagementArea;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File;
import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.service.FileDao;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FileDetailsStore implements EntityStore<File> {

    private final FileDao fileDao;

    private static final Logger lOGGER = LoggerFactory.getLogger(FileDetailsStore.class);

    @Inject
    public FileDetailsStore(FileDao fileDao) {
        this.fileDao = fileDao;
    }

    @Override
    public File create(Map<String, Object> map) throws IOException {
        lOGGER.info("This request is not supported");
        return new File();
    }

    @Override
    public File get(String id) throws IOException {
        return CatalogTypes.fromFile(fileDao.getFileByAbsolutePath(id));
    }

    @Override
    public File update(File fileDetails) throws IOException {
        return new File();
    }

    @Override
    public void delete(String id) throws IOException {
    }

    @Override
    public boolean contains(String id) throws IOException {
        return false;
    }

    @Deprecated
    @Override
    public List<File> search(List<SearchCondition> conditions, long pageIndex, int pageSize) {
        return new ArrayList<>();
    }

    @Override
    public List<File> search(SearchQuery searchQuery, long pageIndex, int pageSize) {
        List<SearchCondition> conditions = searchQuery.getSearchConditions();
        List<File> fileDetails = new ArrayList<>();
        for (SearchCondition con : conditions) {
            String matchPattern = con.getOperand() != null ? String.valueOf(con.getOperand()).trim() : "";
            String selector = con.getFieldName();
            if ("Name".equalsIgnoreCase(selector) || "Extension".equalsIgnoreCase(selector) ||
                    "Owner".equalsIgnoreCase(selector) || "Absolute Path".equalsIgnoreCase(selector) ||
                    "Created By".equalsIgnoreCase(selector) || "Modified By".equalsIgnoreCase(selector) ||
                    "Created".equalsIgnoreCase(selector) || "Modified".equalsIgnoreCase(selector) ||
                    "Base Name".equalsIgnoreCase(selector) || "Size".equalsIgnoreCase(selector)) {
                fileDetails = fileDao.getFilesByFilter(matchPattern, selector, pageIndex, pageSize).stream()
                        .map(CatalogTypes::fromFile).collect(Collectors.toList());
            }
        }
        return fileDetails;
    }

    @Override
    public long count(List<SearchCondition> list) throws IOException {
        return 0;
    }

    @Override
    public List<String> lookup(Map<String, Object> map, List<String> list) throws IOException {
        List<String> stringList = new ArrayList<>();
        return stringList;
    }

}
