package gr.smaragda.dynamiclovtool.model;

import gr.smaragda.dynamiclovtool.enums.SearchFieldValidationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchFieldValidation {
    private String check;
    private SearchFieldValidationType type;
    public void setType(SearchFieldValidationType type) {
        this.type = type;
    }
    public void setType(String type) {
        this.type = SearchFieldValidationType.fromString(type);
    }
    private String error;
    private boolean onSelectValidation;
}
