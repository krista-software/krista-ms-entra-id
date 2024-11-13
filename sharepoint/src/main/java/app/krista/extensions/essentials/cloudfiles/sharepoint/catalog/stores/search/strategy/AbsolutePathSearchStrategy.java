package app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores.search.strategy;

import app.krista.extension.executor.SearchCondition;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores.search.FileEntityCondition;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores.search.FileSearchStrategy;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores.search.impl.FileEntitySearchImpl;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class AbsolutePathSearchStrategy implements FileSearchStrategy {
    @Override
    public List<File> search(FileEntitySearchImpl fileEntitySearch, SearchCondition condition, long pageIndex, int pageSize) {
        String matchPattern = FileEntityCondition.getMatchPattern(condition);
        return fileEntitySearch.getFileByPath(matchPattern)
                .stream()
                .map(File::fromFile)
                .skip(Math.max((pageIndex - 1) * pageSize, 0))
                .limit(pageSize)
                .collect(Collectors.toList());
    }

    @Override
    public List<File> fieldFilter(List<File> fileEntityList, SearchCondition condition) {
        String matchPattern = FileEntityCondition.getMatchPattern(condition);
        String operator = FileEntityCondition.getOperator(condition);
        return fileEntityList
                .stream()
                .filter(file -> !operator.equals(Constants.CONTAINS) || FileEntityCondition.matchTextFieldCondition(file.absolutePath, operator, matchPattern))
                .collect(Collectors.toList());
    }
}
