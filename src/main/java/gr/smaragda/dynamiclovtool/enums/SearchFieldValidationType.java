package gr.smaragda.dynamiclovtool.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum SearchFieldValidationType {
    MATCHES("matches", "Matches", "Η τιμή πρέπει να ταιριάζει με το καθορισμένο regex pattern", true),
    CLEAR("clear", "Clear", "Καθαρισμός του πεδίου από οποιαδήποτε τιμή", true),
    REQUIRED("required", "Required", "Το πεδίο είναι υποχρεωτικό και δεν μπορεί να είναι κενό", false),
    MIN_LENGTH("min_length", "Min Length", "Η τιμή πρέπει να έχει τουλάχιστον τον καθορισμένο αριθμό χαρακτήρων", true),
    MAX_LENGTH("max_length", "Max Length", "Η τιμή δεν μπορεί να υπερβαίνει τον καθορισμένο αριθμό χαρακτήρων", true),
    EMAIL("email", "Email", "Η τιμή πρέπει να είναι έγκυρη διεύθυνση email", false),
    IS_NUMBER("is_number", "Is Number", "Η τιμή πρέπει να είναι αριθμητική (ακέραιος ή δεκαδικός)", false),
    IS_ALPHABETIC("is_alphabetic", "Is Alphabetic", "Η τιμή πρέπει να περιέχει μόνο γράμματα (α-ω, A-Ω, a-z, A-Z)", false),
    MIN_DATE("min_date", "Min Date", "Η ημερομηνία πρέπει να είναι ίδια ή μετά από την καθορισμένη", true),
    MAX_DATE("max_date", "Max Date", "Η ημερομηνία πρέπει να είναι ίδια ή πριν από την καθορισμένη", true);

    private final String type;
    private final String label;
    private final String helpText;
    private final boolean needsCheckValue;

    SearchFieldValidationType(String type, String label, String helpText, boolean needsCheckValue) {
        this.type = type;
        this.label = label;
        this.helpText = helpText;
        this.needsCheckValue = needsCheckValue;
    }

    public static SearchFieldValidationType fromString(String type) {
        return Arrays.stream(values())
                .filter(validationType -> validationType.type.equalsIgnoreCase(type))
                .findFirst()
                .orElseThrow();
    }

    public static List<SearchFieldValidationType> forType(SearchFieldType fieldTypeEnum) {
        if (fieldTypeEnum == null) {
            return List.of(values());
        }

        return switch (fieldTypeEnum) {
            case INPUT -> Arrays.asList(
                    MATCHES,
                    CLEAR,
                    REQUIRED,
                    MIN_LENGTH,
                    MAX_LENGTH,
                    EMAIL,
                    IS_NUMBER,
                    IS_ALPHABETIC
            );
            case DATE -> Arrays.asList(
                    REQUIRED,
                    CLEAR,
                    MIN_DATE,
                    MAX_DATE
            );
            case DROPDOWN, LOV -> Arrays.asList(
                    REQUIRED,
                    CLEAR
            );
            default -> List.of(values());
        };
    }

    @Override
    public String toString() {
        return this.type;
    }
}
