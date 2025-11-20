package gr.smaragda.dynamiclovtool.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultColumn {
    // Getters and Setters
    private String title;
    private String nameInResults;
    private String dataType;
    private boolean visible = true;

    public ResultColumn(String title, String nameInResults) {
        this.title = title;
        this.nameInResults = nameInResults;
    }

}