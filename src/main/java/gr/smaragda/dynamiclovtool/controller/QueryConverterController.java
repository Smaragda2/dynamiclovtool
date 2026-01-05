package gr.smaragda.dynamiclovtool.controller;

import gr.smaragda.dynamiclovtool.enums.SearchFieldType;
import gr.smaragda.dynamiclovtool.enums.SearchFieldValidationType;
import gr.smaragda.dynamiclovtool.model.QueryAnalysis;
import gr.smaragda.dynamiclovtool.model.QueryInput;
import gr.smaragda.dynamiclovtool.model.SearchField;
import gr.smaragda.dynamiclovtool.service.OracleQueryToJsonTool;
import gr.smaragda.dynamiclovtool.util.SessionUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class QueryConverterController {

    private final OracleQueryToJsonTool converterTool;
    private Map<String, String> remainingFieldsMap = new HashMap<>();

    @GetMapping
    public String showForm(Model model, HttpSession session) {
        // Έλεγχος αν υπάρχει analysis στο session
        QueryAnalysis analysis = SessionUtil.getQueryAnalysisOrNull(session);
        if (analysis != null) {
            model.addAttribute("analysis", analysis);
        }

        addDefaultAttributes(model);
        return "index";
    }

    @PostMapping("analyze")
    public String analyzeQuery(@ModelAttribute QueryInput queryInput,
                               HttpSession session,
                               Model model) {

        if (queryInput.getQuery() == null || queryInput.getQuery().trim().isEmpty()) {
            model.addAttribute("error", "Παρακαλώ εισάγετε ένα query");
            addDefaultAttributes(model);
            return "index";
        }

        try {
            QueryAnalysis newAnalysis = converterTool.parseQuery(queryInput.getQuery());
            newAnalysis.setPanelTitle(queryInput.getPanelTitle());
            newAnalysis.setSelectable(queryInput.isSelectable());

            // Αποθήκευση analysis στο session
            session.setAttribute("analysis", newAnalysis);
            model.addAttribute("queryInput", queryInput);
            model.addAttribute("analysis", newAnalysis);
            addDefaultAttributes(model);

            if (newAnalysis.hasErrors()) {
                model.addAttribute("errors", newAnalysis.getValidationErrors());
            }

            if (!newAnalysis.getValidationWarnings().isEmpty()) {
                model.addAttribute("warnings", newAnalysis.getValidationWarnings());
            }

            if (!newAnalysis.hasErrors()) {
                model.addAttribute("success", "Το query αναλύθηκε επιτυχώς!");
            }
        } catch (Exception e) {
            log.error("Error analyzing query", e);
            model.addAttribute("error", "Σφάλμα κατά την ανάλυση του query: " + e.getMessage());
            addDefaultAttributes(model);
        }

        return "index";
    }

    @PostMapping("generate-json")
    public String generateJson(@RequestParam String panelTitle,
                               @RequestParam(defaultValue = "true") boolean selectable,
                               HttpSession session,
                               Model model) {

        try {
            QueryAnalysis analysis = SessionUtil.getQueryAnalysisOrThrowException(session);

            analysis.setPanelTitle(panelTitle);
            analysis.setSelectable(selectable);

            String jsonOutput = converterTool.generateJson(analysis);

            model.addAttribute("generatedJson", jsonOutput);
            model.addAttribute("analysis", analysis);
            addDefaultAttributes(model);
            model.addAttribute("success", "JSON generated successfully!");
        } catch (Exception e) {
            log.error("Error generating JSON", e);
            model.addAttribute("error", "Σφάλμα κατά τη δημιουργία JSON: " + e.getMessage());
            addDefaultAttributes(model);
        }

        return "index";
    }

    @PostMapping("clear-analysis")
    public String clearAnalysis(HttpSession session, Model model) {
        try {
            session.removeAttribute("analysis");
            clearDefaultAttributes(model);

            model.addAttribute("success", "Η ανάλυση καθαρίστηκε επιτυχώς!");

            return "redirect:/";
        } catch (Exception e) {
            log.error("Error clearing analysis", e);
            model.addAttribute("error", "Σφάλμα κατά τον καθαρισμό της ανάλυσης: " + e.getMessage());
            return "redirect:/";
        }
    }

    @GetMapping("/api/validation-types/{fieldType}")
    @ResponseBody
    public Map<String, Object> getValidationTypesForFieldType(@PathVariable String fieldType,
                                                                    @RequestParam(required = false) String fieldDbParam,
                                                                    HttpSession session) {
        try {
            SearchFieldType fieldTypeEnum = SearchFieldType.fromString(fieldType);
            List<SearchFieldValidationType> filteredTypes = SearchFieldValidationType.forType(fieldTypeEnum);

            QueryAnalysis analysis = SessionUtil.getQueryAnalysisOrThrowException(session);
            List<SearchField> otherFields = analysis.getSearchFields()
                    .stream()
                    .filter(searchField -> !searchField.getDbParam().equals(fieldDbParam))
                    .toList();

            String remainingFields;
            if (otherFields.isEmpty()) {
                remainingFields = null;
                filteredTypes = filteredTypes.stream().filter(type -> !SearchFieldValidationType.CLEAR.equals(type)).toList();
            } else {
                remainingFields = otherFields.stream().map(SearchField::getDbParam).collect(Collectors.joining(","));
            }

            if (fieldDbParam != null) {
                remainingFieldsMap.computeIfAbsent(fieldDbParam, k -> remainingFields);
            }

            // Convert to list of maps with all properties
            List<Map<String, Object>> validationTypesList = filteredTypes.stream()
                    .map(vtype -> {
                        Map<String, Object> typeMap = new HashMap<>();
                        typeMap.put("type", vtype.getType());
                        typeMap.put("label", vtype.getLabel());
                        typeMap.put("helpText", vtype.getHelpText());
                        typeMap.put("needsCheckValue", vtype.isNeedsCheckValue());
                        return typeMap;
                    })
                    .toList();

            // Return both validation types and remaining fields
            Map<String, Object> response = new HashMap<>();
            response.put("validationTypes", validationTypesList);
            response.put("remainingFields", remainingFields);

            log.info("Loaded {} validation types for field type: {} with remaining fields: {}",
                    validationTypesList.size(), fieldType, remainingFields);

            return response;

        } catch (IllegalArgumentException e) {
            log.error("Invalid field type: {}", fieldType);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("validationTypes", List.of());
            errorResponse.put("remainingFields", null);
            return errorResponse;
        }
    }

    private void addDefaultAttributes(Model model) {
        if (!model.containsAttribute("queryInput")) {
            model.addAttribute("queryInput", new QueryInput());
        }

        if (!model.containsAttribute("analysis")) {
            model.addAttribute("analysis", new QueryAnalysis());
        }

        addEnumsToModel(model);
    }

    private void clearDefaultAttributes(Model model) {
        model.addAttribute("queryInput", new QueryInput());
        model.addAttribute("analysis", new QueryAnalysis());
        addEnumsToModel(model);
    }

    private void addEnumsToModel(Model model) {
        model.addAttribute("fieldTypes", SearchFieldType.values());
        model.addAttribute("validationTypes", SearchFieldValidationType.forType(null));
    }
}