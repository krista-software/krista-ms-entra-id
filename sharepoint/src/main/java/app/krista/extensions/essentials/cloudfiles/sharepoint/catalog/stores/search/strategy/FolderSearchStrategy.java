package app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores.search.strategy;

import app.krista.extension.executor.SearchCondition;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores.search.FileSearchStrategy;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores.search.impl.FileEntitySearchImpl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FolderSearchStrategy implements FileSearchStrategy {
    @Override
    public List<File> search(FileEntitySearchImpl fileEntitySearch, SearchCondition condition, long pageIndex, int pageSize) {
        return Collections.emptyList();
    }

    @Override
    public List<File> fieldFilter(List<File> fileEntityList, SearchCondition condition) {
        return fileEntityList.stream().filter(file -> file.isFolder).collect(Collectors.toList());
    }
}
