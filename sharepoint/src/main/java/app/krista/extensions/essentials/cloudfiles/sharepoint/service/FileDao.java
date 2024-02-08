package app.krista.extensions.essentials.cloudfiles.sharepoint.service;

import app.krista.extensions.essentials.cloudfiles.sharepoint.impl.DriveImpl;

import java.util.List;
import java.util.Map;

public interface FileDao {

    List<DriveImpl> getFilesByFilter(String name, String selector, long pageIndex, int pageSize);

    FileService getFileByAbsolutePath(String s);

}
