package gr.smaragda.dynamiclovtool.controller;

import gr.smaragda.dynamiclovtool.model.QueryAnalysis;
import gr.smaragda.dynamiclovtool.model.SearchField;
import gr.smaragda.dynamiclovtool.util.SessionUtil;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/api/search-fields")
public class SearchFieldController {

    @PostMapping("/update")
    @ResponseBody
    public String updateField(@RequestParam String dbParam,
                              @RequestBody SearchField updatedField,
                              HttpSession session) {
        try {
            QueryAnalysis analysis = SessionUtil.getQueryAnalysisOrThrowException(session);

            Optional<SearchField> fieldOpt = analysis.getSearchFields().stream()
                    .filter(f -> f.getDbParam().equals(dbParam))
                    .findFirst();

            if (fieldOpt.isPresent()) {
                SearchField existingField = fieldOpt.get();

                // Update basic properties
                if (updatedField.getTitle() != null && !updatedField.getTitle().trim().isEmpty()) {
                    existingField.setTitle(updatedField.getTitle());
                }
                if (updatedField.getType() != null) {
                    existingField.setType(updatedField.getType());
                }
                if (updatedField.getLovCode() != null) {
                    existingField.setLovCode(updatedField.getLovCode());
                }
                existingField.setRequired(updatedField.isRequired());
                existingField.setHidden(updatedField.isHidden());
                existingField.setDisabled(updatedField.isDisabled());

                // Update validations - replace all
                if (updatedField.getValidations() != null) {
                    existingField.getValidations().clear();
                    existingField.getValidations().putAll(updatedField.getValidations());
                }

                // Update dropdown options - replace all
                if (updatedField.getDropdownOptions() != null) {
                    existingField.getDropdownOptions().clear();
                    existingField.getDropdownOptions().addAll(updatedField.getDropdownOptions());
                }

                // Update checkbox field
                if (updatedField.getCheckboxField() != null) {
                    existingField.setCheckboxField(updatedField.getCheckboxField());
                }

                // Update defaultValueSetup
                if (updatedField.getDefaultValueSetup() != null) {
                    existingField.setDefaultValueSetup(updatedField.getDefaultValueSetup());
                }

                log.info("Updated field {} with {} validations and {} dropdown options",
                        dbParam,
                        existingField.getValidations().size(),
                        existingField.getDropdownOptions().size());

                session.setAttribute("analysis", analysis);
                return "OK";
            }
            return "Error: Field not found";
        } catch (Exception e) {
            log.error("Error updating field {}", dbParam, e);
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/{dbParam}")
    @ResponseBody
    public SearchField getField(@PathVariable String dbParam, HttpSession session) {
        try {
            QueryAnalysis analysis = SessionUtil.getQueryAnalysisOrThrowException(session);

            Optional<SearchField> fieldOpt = analysis.getSearchFields().stream()
                    .filter(f -> f.getDbParam().equals(dbParam))
                    .findFirst();

            if (fieldOpt.isPresent()) {
                return fieldOpt.get();
            }
            throw new RuntimeException("Field not found: " + dbParam);
        } catch (Exception e) {
            log.error("Error getting field {}", dbParam, e);
            throw new RuntimeException("Error getting field: " + e.getMessage());
        }
    }
}
