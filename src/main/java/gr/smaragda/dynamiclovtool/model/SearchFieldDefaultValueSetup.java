package gr.smaragda.dynamiclovtool.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import gr.smaragda.dynamiclovtool.enums.SearchFieldValidationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchFieldDefaultValueSetup {
    private String defaultSource;
    private String defaultPath;

    // Constructor for simple value
    public SearchFieldDefaultValueSetup(String defaultSource) {
        this.defaultSource = defaultSource;
    }
}
