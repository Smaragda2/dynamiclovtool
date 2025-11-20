package gr.smaragda.dynamiclovtool.controller;

import gr.smaragda.dynamiclovtool.enums.SearchFieldValidationType;
import gr.smaragda.dynamiclovtool.model.QueryAnalysis;
import gr.smaragda.dynamiclovtool.model.SearchField;
import gr.smaragda.dynamiclovtool.model.SearchFieldValidation;
import gr.smaragda.dynamiclovtool.util.SessionUtil;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/api/search-fields/validations")
public class SearchFieldValidationController {
    @PostMapping("/add")
    public String addValidation(@RequestParam String fieldDbParam,
                                @ModelAttribute SearchFieldValidation validation,
                                HttpSession session,
                                Model model) {

        if (fieldDbParam == null || validation.getType() == null) {
            model.addAttribute("error", "Παρακαλώ επιλέξτε field και validation type");
            return "redirect:/";
        }

        QueryAnalysis analysis = (QueryAnalysis) session.getAttribute("analysis");
        if (analysis == null) {
            model.addAttribute("error", "Παρακαλώ αναλύστε πρώτα ένα query");
            return "redirect:/";
        }

        Optional<SearchField> fieldOpt = analysis.getSearchFields().stream()
                .filter(f -> f.getDbParam().equals(fieldDbParam))
                .findFirst();

        if (fieldOpt.isPresent()) {
            String key = "validation_" + System.currentTimeMillis();
            fieldOpt.get().addValidation(key, validation);
            model.addAttribute("success", "Validation added successfully!");
        } else {
            model.addAttribute("error", "Field not found");
        }

        return "redirect:/";
    }

    @PostMapping("/update")
    @ResponseBody
    public String updateValidation(@RequestParam String fieldDbParam,
                                   @RequestParam String validationKey,
                                   @RequestParam(required = false) String check,
                                   @RequestParam(required = false) String type,
                                   @RequestParam(required = false) String error,
                                   HttpSession session) {

        try {
            QueryAnalysis analysis = SessionUtil.getQueryAnalysisOrThrowException(session);

            Optional<SearchField> fieldOpt = analysis.getSearchFields().stream()
                    .filter(f -> f.getDbParam().equals(fieldDbParam))
                    .findFirst();

            if (fieldOpt.isPresent()) {
                SearchField field = fieldOpt.get();
                SearchFieldValidation validation = field.getValidations().get(validationKey);

                if (validation != null) {
                    if (check != null) validation.setCheck(check);
                    if (type != null) {
                        try {
                            validation.setType(SearchFieldValidationType.valueOf(type));
                        } catch (IllegalArgumentException e) {
                            return "Error: Invalid validation type";
                        }
                    }
                    if (error != null) validation.setError(error);

                    log.info("Updated validation {} for field {}: type={}, check={}",
                            validationKey, fieldDbParam, type, check);
                    return "OK";
                }
                return "Error: Validation not found";
            }
            return "Error: Field not found";
        } catch (Exception e) {
            log.error("Error updating validation {} for field {}", validationKey, fieldDbParam, e);
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/remove")
    @ResponseBody
    public String removeValidation(@RequestParam String fieldDbParam,
                                   @RequestParam String validationKey,
                                   HttpSession session) {

        try {
            QueryAnalysis analysis = SessionUtil.getQueryAnalysisOrThrowException(session);

            Optional<SearchField> fieldOpt = analysis.getSearchFields().stream()
                    .filter(f -> f.getDbParam().equals(fieldDbParam))
                    .findFirst();

            if (fieldOpt.isPresent()) {
                SearchField field = fieldOpt.get();
                field.removeValidation(validationKey);
                log.info("Removed validation {} from field {}", validationKey, fieldDbParam);
                return "OK";
            }
            return "Error: Field not found";
        } catch (Exception e) {
            log.error("Error removing validation {} from field {}", validationKey, fieldDbParam, e);
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/{fieldDbParam}")
    public String showValidations(@PathVariable String fieldDbParam,
                                  Model model,
                                  HttpSession session) {
        try {
            QueryAnalysis analysis = SessionUtil.getQueryAnalysisOrThrowException(session);

            Optional<SearchField> fieldOpt = analysis.getSearchFields().stream()
                    .filter(f -> f.getDbParam().equals(fieldDbParam))
                    .findFirst();

            if (fieldOpt.isPresent()) {
                model.addAttribute("field", fieldOpt.get());
                model.addAttribute("validationTypes", SearchFieldValidationType.values());
                return "fragments/validations-modal :: validationsContent";
            }

            model.addAttribute("error", "Field not found");
            return "fragments/error :: errorContent";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }
}
