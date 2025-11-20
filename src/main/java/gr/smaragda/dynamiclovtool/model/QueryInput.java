package gr.smaragda.dynamiclovtool.model;

import lombok.Data;

@Data
public class QueryInput {
    private String query;
    private String panelTitle = "sidepanel.title.X";
}
