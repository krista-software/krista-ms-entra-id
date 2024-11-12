package app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores;

import app.krista.extension.executor.SearchCondition;
import app.krista.extension.executor.SearchQuery;
import app.krista.extension.util.EntityStore;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores.search.FileSearchStrategy;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores.search.FileSearchStrategyFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores.search.impl.FileEntitySearchImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.service.FileDao;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class FileDetailsStore implements EntityStore<File> {

    private final FileEntitySearchImpl fileEntitySearch;

    private static final Logger lOGGER = LoggerFactory.getLogger(FileDetailsStore.class);
    private final FileDao fileDao;

    @Inject
    public FileDetailsStore(FileEntitySearchImpl fileEntitySearch, FileDao fileDao) {
        this.fileEntitySearch = fileEntitySearch;
        this.fileDao = fileDao;
    }

    @Override
    public File create(Map<String, Object> map) {
        lOGGER.info("This request is not supported");
        return new File();
    }

    @Override
    public File get(String id) {
        return File.fromFile(fileDao.getFileByAbsolutePath(id));
    }

    @Override
    public File update(File fileDetails) {
        return new File();
    }

    @Override
    public void delete(String id) {
        throw new UnsupportedOperationException("This method is not implemented");
    }

    @Override
    public boolean contains(String id) {
        return false;
    }

    @Deprecated
    @Override
    public List<File> search(List<SearchCondition> conditions, long pageIndex, int pageSize) {
        return new ArrayList<>();
    }

    @Override
    public List<File> search(SearchQuery searchQuery, long pageIndex, int pageSize) throws IOException {
        List<SearchCondition> conditions = searchQuery.getSearchConditions();
        List<File> fileDetails = new ArrayList<>();
        if (conditions.size() > 1) {
            fetchFilesBySearchCondition(pageIndex, pageSize, conditions, fileDetails);
        }

        fileDetails = filterFileByConditions(conditions, fileDetails);

        if (conditions.size() == 1 && fileDetails.isEmpty()) {
            fileDetails = fileDao.getFilesByFilter(null, Constants.LOGICAL_NAME, Constants.ALL, pageIndex, pageSize).stream()
                    .map(File::fromFile)
                    .collect(Collectors.toList());
        }
        return fileDetails;
    }

    private static List<File> filterFileByConditions(List<SearchCondition> conditions, List<File> fileDetails) {
        for (SearchCondition condition : conditions) {
            FileSearchStrategy searchStrategy = FileSearchStrategyFactory.getStrategy(condition);
            if (searchStrategy == null) {
                continue;
            }
            fileDetails = searchStrategy.fieldFilter(fileDetails, condition);

            if (!condition.getFieldName().equals(Constants.LOGICAL_NAME) && fileDetails.isEmpty()) {
                break;
            }
        }
        return fileDetails;
    }

    private void fetchFilesBySearchCondition(long pageIndex, int pageSize, List<SearchCondition> conditions, List<File> fileDetails) throws IOException {
        boolean flag = true;
        for (SearchCondition condition : conditions) {
            if (condition.getFieldName().equals(FileSearchStrategyFactory.ABSOLUTE_PATH) || condition.getFieldName().equals(FileSearchStrategyFactory.PARENT_FOLDER)) {
                FileSearchStrategy searchStrategy = FileSearchStrategyFactory.getStrategy(condition);
                fileDetails.addAll(Objects.requireNonNull(searchStrategy).search(fileEntitySearch, condition, pageIndex, pageSize));
                flag = false;
            }
        }
        if (flag) {
            fileDetails.addAll(fileDao.getFilesByFilter(conditions, null, null, pageIndex, pageSize).stream().map(File::fromFile)
                    .collect(Collectors.toList()));
        }
    }

    @Override
    public long count(List<SearchCondition> list) {
        return 0;
    }

    @Override
    public List<String> lookup(Map<String, Object> map, List<String> list) {
        return new ArrayList<>();
    }

}
