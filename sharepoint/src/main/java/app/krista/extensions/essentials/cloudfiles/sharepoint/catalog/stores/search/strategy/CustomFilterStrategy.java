package app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores.search.strategy;

import app.krista.extension.executor.SearchCondition;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores.search.FileEntityCondition;
import app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores.search.FileSearchStrategyFactory;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.DriveUtil;
import com.microsoft.graph.models.SearchQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CustomFilterStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomFilterStrategy.class);
    public static final String FILENAME = "filename";

    private CustomFilterStrategy() {
    }

    public static void prepareSearchQuery(String fieldName, String matchPattern, SearchQuery query) {
        prepareSearchQuery(null, query, fieldName, matchPattern);
    }

    public static void prepareSearchQuery(SearchCondition condition, SearchQuery query) {
        prepareSearchQuery(condition, query, null, null);
    }

    public static void prepareSearchQuery(SearchCondition condition, SearchQuery query, String name, String fieldValue) {
        String matchPattern = fieldValue == null ? FileEntityCondition.getMatchPattern(condition) : fieldValue;
        String fieldName = name == null ? condition.getFieldName() : name;

        switch (fieldName) {
            case FileSearchStrategyFactory.NAME:
            case FileSearchStrategyFactory.BASE_NAME:
                if (query.queryString.contains(FILENAME)) {
                    break;
                }
                query.queryString = appendQueryString(query, matchPattern, FILENAME);
                break;
            case FileSearchStrategyFactory.EXTENSION:
                query.queryString = appendQueryString(query, matchPattern, "filetype");
                break;
            case FileSearchStrategyFactory.OWNER:
                query.queryString = appendQueryString(query, matchPattern, "createdby");
                break;
            case FileSearchStrategyFactory.MODIFIED:
                String operatorModified = FileEntityCondition.getOperator(condition);
                query.queryString = appendQueryWithNumericValue(query, operatorModified, matchPattern, "lastmodifiedtime", true);
                break;
            case FileSearchStrategyFactory.CREATED:
                String operatorCreated = FileEntityCondition.getOperator(condition);
                query.queryString = appendQueryWithNumericValue(query, operatorCreated, matchPattern, "created", true);
                break;
            case Constants.SIZE:
                String operatorSize = FileEntityCondition.getOperator(condition);
                query.queryString = appendQueryWithNumericValue(query, operatorSize, matchPattern, "size", false);
                break;
            default:
                break;
        }
    }

    private static String appendQueryWithNumericValue(SearchQuery query, String operator, String matchPattern, String fieldType, boolean isDate) {
        return query.queryString + " AND " + prepareQueryWithOperand(operator, matchPattern, fieldType, isDate);
    }

    private static String prepareQueryWithOperand(String operator, String matchPattern, String fieldType, boolean isDate) {
        String updatedValue = isDate ? DriveUtil.doubleToDateConversion(matchPattern) : matchPattern;

        switch (operator) {
            case Constants.GTE:
                return fieldType + ">=" + (isDate ? updatedValue : getLongNumberValue(updatedValue));
            case Constants.LTE:
                return fieldType + "<=" + (isDate ? updatedValue : getLongNumberValue(updatedValue));
            case Constants.GT:
                return fieldType + ">" + (isDate ? updatedValue : getLongNumberValue(updatedValue));
            case Constants.LT:
                return fieldType + "<" + (isDate ? updatedValue : getLongNumberValue(updatedValue));
            case Constants.EQUALS:
                return fieldType + "=" + (isDate ? updatedValue : getLongNumberValue(updatedValue));
            default:
                return "";
        }
    }

    private static long getLongNumberValue(String updatedValue) {
        try {
            double doubleNumber = Double.parseDouble(updatedValue);
            return (long) doubleNumber;
        } catch (NumberFormatException casue) {
            LOGGER.error("Error parsing the string to long: {} ", casue.getMessage());
        }
        return 0;
    }

    private static String appendQueryString(SearchQuery query, String matchPattern, String fieldType) {
        return query.queryString + " AND " + fieldType + ":\"" + matchPattern + "\"";
    }


    public static boolean excludeConditions(SearchCondition condition) {
        Set<String> set = new HashSet<>(Arrays.asList(
                FileSearchStrategyFactory.MODIFIED_BY,
                FileSearchStrategyFactory.CREATED_BY,
                FileSearchStrategyFactory.IS_FOLDER,
                FileSearchStrategyFactory.ABSOLUTE_PATH,
                FileSearchStrategyFactory.PARENT_FOLDER,
                Constants.LOGICAL_NAME
        ));
        return set.contains(condition.getFieldName());
    }
}
