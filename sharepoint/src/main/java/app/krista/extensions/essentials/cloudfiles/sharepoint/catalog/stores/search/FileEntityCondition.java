package app.krista.extensions.essentials.cloudfiles.sharepoint.catalog.stores.search;

import app.krista.extension.executor.SearchCondition;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.Constants;
import app.krista.extensions.essentials.cloudfiles.sharepoint.util.FileUtil;
import org.jetbrains.annotations.NotNull;

public class FileEntityCondition {

    private FileEntityCondition() {
    }

    public static boolean matchTextFieldCondition(String fieldName, String operator, String matchPattern) {
        if (fieldName == null || matchPattern == null) {
            return false;
        }
        switch (operator) {
            case Constants.EQUALS:
                return fieldName.equals(matchPattern);
            case Constants.STARTSWITH:
                return fieldName.startsWith(matchPattern);
            case Constants.ENDSWITH:
                return fieldName.endsWith(matchPattern);
            case Constants.CONTAINS:
                return containsIgnoreCase(fieldName,matchPattern);
            case Constants.NOTEQUALS:
                return !fieldName.equals(matchPattern);
            default:
                return false;
        }
    }

    public static boolean matchNumberFieldCondition(Number entityFieldValue, String operator, Number fieldValue) {

        switch (operator) {
            case Constants.GTE:
                return entityFieldValue instanceof Long ? ((Long) entityFieldValue >= (Long) fieldValue) : ((Double) entityFieldValue >= (Double) fieldValue);
            case Constants.LTE:
                return entityFieldValue instanceof Long ? ((Long) entityFieldValue <= (Long) fieldValue) : ((Double) entityFieldValue <= (Double) fieldValue);
            case Constants.GT:
                return entityFieldValue instanceof Long ? ((Long) entityFieldValue > (Long) fieldValue) : ((Double) entityFieldValue > (Double) fieldValue);
            case Constants.LT:
                return entityFieldValue instanceof Long ? ((Long) entityFieldValue < (Long) fieldValue) : ((Double) entityFieldValue < (Double) fieldValue);
            case Constants.EQUALS:
                return entityFieldValue instanceof Long ? FileUtil.isEqualDate((Long) entityFieldValue, (Long) fieldValue) : entityFieldValue.equals(fieldValue);
            default:
                return false;
        }
    }

    @NotNull
    public static String getOperator(SearchCondition condition) {
        return condition.getOperator() != null ? String.valueOf(condition.getOperator()).trim() : "";
    }

    public static double getSizeInDouble(SearchCondition condition) {
        return condition.getOperand() != null ? (Double) condition.getOperand() : 0;
    }

    @NotNull
    public static String getMatchPattern(SearchCondition condition) {
        return condition.getOperand() != null ? String.valueOf(condition.getOperand()).trim() : "";
    }

    public static boolean containsIgnoreCase(String fieldValue, String substring) {
        if (fieldValue == null || substring == null) {
            return false;
        }

        return fieldValue.toLowerCase().contains(substring.toLowerCase());
    }
}
