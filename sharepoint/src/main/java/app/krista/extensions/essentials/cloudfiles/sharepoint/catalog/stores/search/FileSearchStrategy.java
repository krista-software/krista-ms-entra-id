package app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores.search;

import app.krista.extension.executor.SearchCondition;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.entities.File;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores.search.impl.FileEntitySearchImpl;


import java.io.IOException;
import java.util.List;

public interface FileSearchStrategy {
    List<File> search(FileEntitySearchImpl fileEntitySearch, SearchCondition condition, long pageIndex, int pageSize) throws IOException;
    List<File> fieldFilter(List<File> fileEntityList, SearchCondition condition);
}
