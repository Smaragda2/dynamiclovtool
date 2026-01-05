package gr.smaragda.dynamiclovtool.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchFieldCheckbox {
    private String checkedValue = "1";
    private String uncheckedValue = "0";
    private String initialValue = "0";
}
