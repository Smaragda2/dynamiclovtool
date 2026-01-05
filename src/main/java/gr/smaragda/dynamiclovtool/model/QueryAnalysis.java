package gr.smaragda.dynamiclovtool.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class QueryAnalysis {
    private String panelTitle;
    private boolean selectable = true; //Displays the arrow on each row to select the specific result
    private String originalQuery;
    private List<SearchField> searchFields = new ArrayList<>();
    private List<ResultColumn> resultColumns = new ArrayList<>();
    private List<String> validationErrors = new ArrayList<>();
    private List<String> validationWarnings = new ArrayList<>();

    public boolean hasErrors() {
        return !validationErrors.isEmpty();
    }

    public void addSearchField(SearchField field) {
        this.searchFields.add(field);
    }

    public void addResultColumn(ResultColumn column) {
        this.resultColumns.add(column);
    }

    public void addError(String error) {
        this.validationErrors.add(error);
    }

    public void addWarning(String warning) {
        this.validationWarnings.add(warning);
    }
}