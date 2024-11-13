package app.krista.extensions.essentials.cloudfiles.sharepoint.service;

import app.krista.extension.executor.SearchCondition;

import java.util.List;

public interface FileDao {


    List<FileService> getFilesByFilter(List<SearchCondition> conditions, String name, String selector, long pageIndex, int pageSize);

    FileService getFileByAbsolutePath(String s);

}
