package gr.smaragda.dynamiclovtool.service;

import gr.smaragda.dynamiclovtool.enums.SearchFieldType;
import gr.smaragda.dynamiclovtool.model.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OracleQueryToJsonTool {

    public QueryAnalysis parseQuery(String query) {
        QueryAnalysis analysis = new QueryAnalysis();
        analysis.setOriginalQuery(query);

        try {
            extractSearchFields(query, analysis);
            extractResultColumns(query, analysis);
            validateQueryStructure(query, analysis);

        } catch (Exception e) {
            log.error("Error parsing query", e);
            analysis.addError("Σφάλμα κατά την ανάλυση του query: " + e.getMessage());
        }

        return analysis;
    }

    private void extractSearchFields(String query, QueryAnalysis analysis) {
        Pattern paramPattern = Pattern.compile(":(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = paramPattern.matcher(query);

        Set<String> uniqueParams = new LinkedHashSet<>();
        while (matcher.find()) {
            String param = matcher.group(1);
            if (!isInStringLiteral(query, matcher.start())) {
                uniqueParams.add(param.toUpperCase());
            }
        }

        for (String param : uniqueParams) {
            SearchField field = new SearchField();
            field.setDbParam(param);
            field.setRequired(isParameterRequired(query, param));
            field.setTitle(generateDefaultTitle(param));
            field.setType(detectFieldType(param, query));

            analysis.addSearchField(field);
        }

        analysis.getSearchFields().sort(Comparator.comparing(SearchField::getDbParam));
    }

    private boolean isInStringLiteral(String query, int position) {
        String substring = query.substring(0, position);
        int singleQuotes = countOccurrences(substring, "'");
        return singleQuotes % 2 != 0;
    }

    private int countOccurrences(String str, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }

    private boolean isParameterRequired(String query, String param) {
        String upperQuery = query.toUpperCase();
        String paramPattern = ":" + param.toUpperCase();

        boolean hasNvl = Pattern.compile("NVL\\s*\\(\\s*" + paramPattern, Pattern.CASE_INSENSITIVE).matcher(upperQuery).find();
        boolean hasCoalesce = Pattern.compile("COALESCE\\s*\\(\\s*" + paramPattern, Pattern.CASE_INSENSITIVE).matcher(upperQuery).find();
        boolean hasIsNullCheck = Pattern.compile(paramPattern + "\\s+IS\\s+NULL", Pattern.CASE_INSENSITIVE).matcher(upperQuery).find();
        boolean hasDefault = Pattern.compile("=\\s*" + paramPattern, Pattern.CASE_INSENSITIVE).matcher(upperQuery).find();
        boolean hasLike = Pattern.compile("LIKE\\s*" + paramPattern, Pattern.CASE_INSENSITIVE).matcher(upperQuery).find();

        return !(hasNvl || hasCoalesce || hasIsNullCheck || hasDefault || hasLike);
    }

    private SearchFieldType detectFieldType(String param, String query) {
        String upperQuery = query.toUpperCase();
        String paramPattern = ":" + param.toUpperCase();

        if (Pattern.compile("TO_DATE\\s*\\(\\s*" + paramPattern, Pattern.CASE_INSENSITIVE).matcher(upperQuery).find()) {
            return SearchFieldType.DATE;
        }

        if (param.toUpperCase().contains("LOV")) {
            return SearchFieldType.LOV;
        }

        return SearchFieldType.INPUT;
    }

    private String generateDefaultTitle(String dbParam) {
        String withoutP = dbParam.replaceFirst("^P_?", "");
        return "sidepanel.title." + Arrays.stream(withoutP.split("_"))
                .map(word -> {
                    if (word.isEmpty()) return "";
                    return word.substring(0, 1).toUpperCase() +
                            word.substring(1).toLowerCase();
                })
                .collect(Collectors.joining(""));
    }

    private void extractResultColumns(String query, QueryAnalysis analysis) {
        String selectPart = extractSelectPart(query);
        if (selectPart == null) {
            analysis.addError("Δεν βρέθηκε το SELECT μέρος του query");
            return;
        }

        log.debug("SELECT part: {}", selectPart);

        // Χωρισμός columns με πιο έξυπνο τρόπο
        List<String> columns = splitSelectColumns(selectPart);

        log.debug("Found {} columns", columns.size());

        for (String column : columns) {
            String trimmed = column.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            log.debug("Processing column: {}", trimmed);

            if (trimmed.toUpperCase().startsWith("JSON_OBJECT")) {
                extractJsonObjectColumns(trimmed, analysis);
            } else {
                extractSimpleColumn(trimmed, analysis);
            }
        }
    }

    private List<String> splitSelectColumns(String selectPart) {
        List<String> columns = new ArrayList<>();
        StringBuilder currentColumn = new StringBuilder();
        int parenDepth = 0;
        int singleQuoteDepth = 0;
        boolean inJsonObject = false;

        for (int i = 0; i < selectPart.length(); i++) {
            char c = selectPart.charAt(i);
            char nextChar = (i < selectPart.length() - 1) ? selectPart.charAt(i + 1) : '\0';

            if (c == '\'') {
                singleQuoteDepth = 1 - singleQuoteDepth;
            } else if (c == '(' && singleQuoteDepth == 0) {
                parenDepth++;
                if (i > 0 && selectPart.substring(Math.max(0, i - 11), i + 1).toUpperCase().contains("JSON_OBJECT")) {
                    inJsonObject = true;
                }
            } else if (c == ')' && singleQuoteDepth == 0) {
                parenDepth--;
                if (parenDepth == 0) {
                    inJsonObject = false;
                }
            }

            // Split on comma only when not inside parentheses, JSON object, or quotes
            if (c == ',' && parenDepth == 0 && singleQuoteDepth == 0 && !inJsonObject) {
                String column = currentColumn.toString().trim();
                if (!column.isEmpty()) {
                    columns.add(column);
                }
                currentColumn = new StringBuilder();
                continue;
            }

            currentColumn.append(c);
        }

        // Add the last column
        String lastColumn = currentColumn.toString().trim();
        if (!lastColumn.isEmpty()) {
            columns.add(lastColumn);
        }

        return columns;
    }

    private String extractSelectPart(String query) {
        // Καλύτερος καθαρισμός comments
        String cleaned = query
                .replaceAll("/\\*.*?\\*/", " ")  // Remove multi-line comments
                .replaceAll("--[^\\n]*", " ")    // Remove single-line comments
                .replaceAll("\\s+", " ")         // Normalize whitespace
                .trim();

        // Βελτιωμένο regex για SELECT clause
        Pattern selectPattern = Pattern.compile(
                "SELECT\\s+(.*?)(?=\\s+FROM\\s+|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );

        Matcher matcher = selectPattern.matcher(cleaned);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // Εναλλακτική προσέγγιση αν το πρώτο regex αποτύχει
        return extractSelectPartAlternative(cleaned);
    }

    private String extractSelectPartAlternative(String query) {
        try {
            int selectIndex = query.toUpperCase().indexOf("SELECT");
            if (selectIndex == -1) return null;

            int fromIndex = query.toUpperCase().indexOf("FROM", selectIndex);
            if (fromIndex == -1) return null;

            return query.substring(selectIndex + 6, fromIndex).trim();
        } catch (Exception e) {
            log.error("Error in alternative SELECT extraction", e);
            return null;
        }
    }

    private void extractJsonObjectColumns(String jsonExpr, QueryAnalysis analysis) {
        try {
            // Αφαίρεση του "JSON_OBJECT(" από την αρχή και ")" από το τέλος
            String content = jsonExpr.replaceFirst("(?i)JSON_OBJECT\\s*\\(", "").trim();
            if (content.toUpperCase().endsWith(") ADD_JSON")) {
                content = content.substring(0, content.length() - ") ADD_JSON".length()).trim();
            }

            log.debug("JSON content: {}", content);

            // Χωρισμός βασισμένος σε commas που δεν είναι μέσα σε parentheses
            List<String> keyValuePairs = splitJsonKeyValuePairs(content);

            log.debug("Found {} key-value pairs", keyValuePairs.size());

            for (String pair : keyValuePairs) {
                // Χωρισμός key:value
                String[] parts = pair.split(":", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String valueExpr = parts[1].trim();

                    // Αφαίρεση single quotes από το key
                    if (key.startsWith("'") && key.endsWith("'")) {
                        key = key.substring(1, key.length() - 1);
                    }

                    if (!key.isEmpty()) {
                        log.debug("Found JSON key: '{}' with value: '{}'", key, valueExpr);

                        if (containsGreek(key)) {
                            analysis.addError("JSON key περιέχει ελληνικά: '" + key + "'");
                        }

                        ResultColumn column = new ResultColumn(generateColumnTitle(key), key);
                        analysis.addResultColumn(column);
                    }
                } else if (parts.length == 1) {
                    String key = parts[0].trim();
                    extractSimpleColumn(key, analysis);
                }
            }
        } catch (Exception e) {
            log.error("Error parsing JSON object: {}", jsonExpr, e);
            analysis.addError("Σφάλμα ανάλυσης JSON object: " + e.getMessage());
        }
    }

    private List<String> splitJsonKeyValuePairs(String content) {
        List<String> pairs = new ArrayList<>();
        StringBuilder currentPair = new StringBuilder();
        int parenDepth = 0;
        int singleQuoteDepth = 0;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (c == '\'') {
                singleQuoteDepth = 1 - singleQuoteDepth;
            } else if (c == '(' && singleQuoteDepth == 0) {
                parenDepth++;
            } else if (c == ')' && singleQuoteDepth == 0) {
                parenDepth--;
            }

            // Split on comma only when not inside parentheses or quotes
            if (c == ',' && parenDepth == 0 && singleQuoteDepth == 0) {
                String pair = currentPair.toString().trim();
                if (!pair.isEmpty()) {
                    pairs.add(pair);
                }
                currentPair = new StringBuilder();
                continue;
            }

            currentPair.append(c);
        }

        // Add the last pair
        String lastPair = currentPair.toString().trim();
        if (!lastPair.isEmpty()) {
            pairs.add(lastPair);
        }

        return pairs;
    }

    private void extractSimpleColumn(String column, QueryAnalysis analysis) {
        String alias = extractAlias(column);
        if (alias == null || alias.trim().isEmpty()) {
            // Αν δεν βρέθηκε alias, χρησιμοποιούμε το τελευταιο μέρος της έκφρασης
            alias = extractLastIdentifier(column);
        }

        if (alias == null || alias.trim().isEmpty()) {
            analysis.addError("Column missing alias: " + column);
            return;
        }

        alias = cleanColumnName(alias);

        if (containsGreek(alias)) {
            analysis.addError("Column alias περιέχει ελληνικά: '" + alias + "'");
        }

        if (hasConversionWithoutLabel(column)) {
            analysis.addWarning("Conversion/Concat detected without proper alias: " + column);
        }

        ResultColumn resultCol = new ResultColumn(generateColumnTitle(alias), alias);
        analysis.addResultColumn(resultCol);

        log.debug("Added column: {} -> {}", alias, generateColumnTitle(alias));
    }

    private String extractLastIdentifier(String expression) {
        // Προσπαθεί να βρει το τελευταιο identifier (όνομα στήλης ή πίνακα)
        String[] parts = expression.split("[\\s,\\.\\(\\)]");
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i].trim();
            if (!part.isEmpty() && !isSqlKeyword(part) && !part.matches("^[\\d\\W]+$")) {
                return part;
            }
        }
        return null;
    }

    private boolean isSqlKeyword(String word) {
        String[] keywords = {"SELECT", "FROM", "WHERE", "AND", "OR", "AS", "JSON_OBJECT", "VALUE", "NVL", "TO_CHAR", "TO_DATE", "CASE", "WHEN", "THEN", "ELSE", "END", "LIKE", "IS", "NULL"};
        String upperWord = word.toUpperCase();
        for (String keyword : keywords) {
            if (keyword.equals(upperWord)) {
                return true;
            }
        }
        return false;
    }

    private String cleanColumnName(String columnName) {
        // Remove table references like "a.", "table.", etc.
        return columnName.replaceAll("^[a-zA-Z_][a-zA-Z0-9_]*\\.", "");
    }

    private boolean containsGreek(String text) {
        return Pattern.compile("[α-ωΑ-Ω]").matcher(text).find();
    }

    private boolean hasConversionWithoutLabel(String expression) {
        return Pattern.compile("(TO_CHAR|TO_DATE|TO_NUMBER|\\|\\|)").matcher(expression.toUpperCase()).find() &&
                !expression.toUpperCase().contains(" AS ");
    }

    private String extractAlias(String column) {
        // First try to extract alias with AS
        Pattern aliasPattern = Pattern.compile("(?i)AS\\s+[\"']?([\\w_]+)[\"']?$");
        Matcher matcher = aliasPattern.matcher(column);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // If no AS, try to get the last part (could be column name or alias)
        String[] parts = column.split("\\s+");
        if (parts.length > 0) {
            String lastPart = parts[parts.length - 1].trim();
            // Remove any trailing commas or other punctuation
            lastPart = lastPart.replaceAll("[,\\);]+$", "");
            return lastPart;
        }

        return null;
    }

    private String generateColumnTitle(String alias) {
        return "sidepanel.field." + Arrays.stream(alias.split("_"))
                .map(word -> {
                    if (word.isEmpty()) return "";
                    return word.substring(0, 1).toUpperCase() +
                            word.substring(1).toLowerCase();
                })
                .collect(Collectors.joining(""));
    }

    private void validateQueryStructure(String query, QueryAnalysis analysis) {
        if (!query.toUpperCase().contains("SELECT")) {
            analysis.addError("Query must contain SELECT clause");
        }

        if (!query.toUpperCase().contains("FROM")) {
            analysis.addError("Query must contain FROM clause");
        }

        if (query.toUpperCase().contains("DELETE") || query.toUpperCase().contains("UPDATE") ||
                query.toUpperCase().contains("INSERT") || query.toUpperCase().contains("DROP")) {
            analysis.addError("Query contains potentially dangerous operations");
        }
    }

    public String generateJson(QueryAnalysis analysis) {
        try {
            Map<String, Object> jsonMap = new LinkedHashMap<>();
            jsonMap.put("panelTitle", analysis.getPanelTitle());

            // Search Fields
            Map<String, Object> searchFieldsMap = new LinkedHashMap<>();
            for (SearchField field : analysis.getSearchFields()) {
                Map<String, Object> fieldMap = new LinkedHashMap<>();
                fieldMap.put("dbParam", field.getDbParam());
                fieldMap.put("type", field.getType().getType());
                fieldMap.put("required", field.isRequired());
                fieldMap.put("visible", !field.isHidden());

                // Validations
                if (!field.getValidations().isEmpty()) {
                    Map<String, Object> validationsMap = new LinkedHashMap<>();
                    for (Map.Entry<String, SearchFieldValidation> entry : field.getValidations().entrySet()) {
                        Map<String, Object> validationMap = new LinkedHashMap<>();
                        validationMap.put("check", entry.getValue().getCheck());
                        validationMap.put("type", entry.getValue().getType().getType());
                        validationMap.put("error", entry.getValue().getError());
                        validationMap.put("onSelectValidation", entry.getValue().isOnSelectValidation());
                        validationsMap.put(entry.getKey(), validationMap);
                    }
                    fieldMap.put("validations", validationsMap);
                }

                // LOV specific
                if (field.getType() == SearchFieldType.LOV && field.getLovCode() != null) {
                    fieldMap.put("lovCode", field.getLovCode());
                }

                // Dropdown specific
                if (field.getType() == SearchFieldType.DROPDOWN && !field.getDropdownOptions().isEmpty()) {
                    List<Map<String, String>> optionsList = new ArrayList<>();
                    for (SearchFieldDropdownOption option : field.getDropdownOptions()) {
                        Map<String, String> optionMap = new HashMap<>();
                        optionMap.put("label", option.getLabel());
                        optionMap.put("value", option.getValue());
                        optionsList.add(optionMap);
                    }
                    fieldMap.put("options", optionsList);
                }

                searchFieldsMap.put(field.getTitle(), fieldMap);
            }
            jsonMap.put("searchFields", searchFieldsMap);

            // Result Columns
            Map<String, Object> resultColumnsMap = new LinkedHashMap<>();
            int i = 0;
            String nameInResults;
            for (ResultColumn column : analysis.getResultColumns()) {
                if (column.isVisible()) {
                    nameInResults = getNameInResults(i, column.getNameInResults());
                    resultColumnsMap.put(column.getTitle(), nameInResults);
                    i++;
                }

            }
            jsonMap.put("resultColumns", resultColumnsMap);

            return convertToJsonString(jsonMap);

        } catch (Exception e) {
            log.error("Error generating JSON", e);
            return "{\"error\": \"Failed to generate JSON: " + e.getMessage() + "\"}";
        }
    }

    private String getNameInResults(int i, String nameInResults) {
        if (i == 0) {
            return "code";
        }
        return i == 1 ? "description" : nameInResults;
    }

    private String convertToJsonString(Map<String, Object> map) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");

        List<String> keys = new ArrayList<>(map.keySet());
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            json.append("\t\"").append(key).append("\" : ");

            Object value = map.get(key);
            if (value instanceof String) {
                json.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Map) {
                json.append(convertNestedMapToJson((Map<?, ?>) value, 2));
            } else {
                json.append(value);
            }

            if (i < keys.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("}");
        return json.toString();
    }

    private String convertNestedMapToJson(Map<?, ?> map, int indentLevel) {
        StringBuilder json = new StringBuilder();
        String indent = "  ".repeat(indentLevel);

        json.append("{\n");

        List<?> keys = new ArrayList<>(map.keySet());
        for (int i = 0; i < keys.size(); i++) {
            Object key = keys.get(i);
            json.append(indent).append("\"").append(key).append("\" : ");

            Object value = map.get(key);
            if (value instanceof String) {
                json.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Map) {
                json.append(convertNestedMapToJson((Map<?, ?>) value, indentLevel + 1));
            } else if (value instanceof List) {
                json.append(convertListToJson((List<?>) value, indentLevel + 1));
            } else {
                json.append(value);
            }

            if (i < keys.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ".repeat(indentLevel - 1)).append("}");
        return json.toString();
    }

    private String convertListToJson(List<?> list, int indentLevel) {
        StringBuilder json = new StringBuilder();
        String indent = "  ".repeat(indentLevel);

        json.append("[\n");

        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            json.append(indent);

            if (item instanceof Map) {
                json.append(convertNestedMapToJson((Map<?, ?>) item, indentLevel + 1));
            } else if (item instanceof String) {
                json.append("\"").append(escapeJson((String) item)).append("\"");
            } else {
                json.append(item);
            }

            if (i < list.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ".repeat(indentLevel - 1)).append("]");
        return json.toString();
    }

    private String escapeJson(String str) {
        if (str == null) return null;
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}