package gr.smaragda.dynamiclovtool.enums;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

import java.util.Arrays;

@Getter
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum SearchFieldType {
    INPUT("input"),
    DATE("date"),
    DROPDOWN("dropdown"),
    LOV("lov");

    private final String type;

    SearchFieldType(String type) {
        this.type = type;
    }

    public static SearchFieldType fromString(String type) {
        return Arrays.stream(values())
                .filter(jsonType -> jsonType.type.equalsIgnoreCase(type))
                .findFirst()
                .orElseThrow();
    }

    @Override
    public String toString() {
        return this.type;
    }
}
