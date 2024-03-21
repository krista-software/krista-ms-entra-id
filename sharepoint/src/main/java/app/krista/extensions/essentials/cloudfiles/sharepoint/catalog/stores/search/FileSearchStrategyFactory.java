package app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores.search;

import app.krista.extension.executor.SearchCondition;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores.search.strategy.*;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;

public class FileSearchStrategyFactory {
    public static final String MODIFIED = "Modified";
    public static final String CREATED = "Created";
    public static final String NAME = "Name";
    public static final String EXTENSION = "Extension";
    public static final String BASE_NAME = "Base Name";
    public static final String CREATED_BY = "Created By";
    public static final String MODIFIED_BY = "Modified By";
    public static final String OWNER = "Owner";
    public static final String PARENT_FOLDER = "Parent Folder";
    public static final String ABSOLUTE_PATH = "Absolute Path";
    public static final String IS_FOLDER = "Is Folder";

    private FileSearchStrategyFactory() {
    }

    public static FileSearchStrategy getStrategy(SearchCondition condition) {
        String fieldName = condition.getFieldName();
        switch (fieldName) {
            case Constants.SIZE:
                return new SizeSearchStrategy();
            case MODIFIED:
                return new DateSearchStrategy();
            case CREATED:
                return new DateSearchStrategy();
            case NAME:
                return new NameSearchStrategy();
            case BASE_NAME:
                return new BaseNameSearchStrategy();
            case EXTENSION:
                return new ExtensionSearchStrategy();
            case IS_FOLDER:
                return new FolderSearchStrategy();
            case OWNER:
                return new OwnerSearchStrategy();
            case PARENT_FOLDER:
                return new ParentPathSearchStrategy();
            case ABSOLUTE_PATH:
                return new AbsolutePathSearchStrategy();
            default:
                return null;
        }
    }
}
