package gr.smaragda.dynamiclovtool.model;

import gr.smaragda.dynamiclovtool.enums.SearchFieldType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchField {
    // Getters and Setters
    private String title;
    private String dbParam;
    private SearchFieldType type = SearchFieldType .INPUT;
    public void setType(SearchFieldType type) {
        this.type = type;
    }
    public void setType(String type) {
        this.type = SearchFieldType.fromString(type);
    }

    private boolean hidden = false;
    private boolean required = false;
    private boolean disabled = false;
    private Map<String, SearchFieldValidation> validations = new LinkedHashMap<>();
    private String lovCode;
    private List<SearchFieldDropdownOption> dropdownOptions = new ArrayList<>();
    private SearchFieldCheckbox checkboxField;
    private SearchFieldDefaultValueSetup defaultValueSetup;

    // Constructors
    public SearchField(String dbParam, String title) {
        this.dbParam = dbParam;
        this.title = title;
    }

    // Helper methods
    public void addValidation(String key, SearchFieldValidation validation) {
        this.validations.put(key, validation);
    }

    public void addDropdownOption(String label, String value) {
        this.dropdownOptions.add(new SearchFieldDropdownOption(label, value));
    }

    public void removeValidation(String key) {
        this.validations.remove(key);
    }
}