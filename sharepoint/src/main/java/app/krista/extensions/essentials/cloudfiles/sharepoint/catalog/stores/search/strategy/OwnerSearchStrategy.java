package app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores.search.strategy;

import app.krista.extension.executor.SearchCondition;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores.search.FileEntityCondition;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores.search.FileSearchStrategy;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores.search.impl.FileEntitySearchImpl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class OwnerSearchStrategy implements FileSearchStrategy {
    @Override
    public List<File> search(FileEntitySearchImpl fileEntitySearch, SearchCondition condition, long pageIndex, int pageSize) throws IOException {
        return Collections.emptyList();
    }

    @Override
    public List<File> fieldFilter(List<File> fileEntityList, SearchCondition condition) {
        String matchPattern = FileEntityCondition.getMatchPattern(condition);
        String operator = FileEntityCondition.getOperator(condition);
        return fileEntityList.stream().filter(file -> FileEntityCondition.matchTextFieldCondition(file.owner, operator, matchPattern)).collect(Collectors.toList());
    }
}
