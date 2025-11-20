package gr.smaragda.dynamiclovtool.controller;

import gr.smaragda.dynamiclovtool.model.QueryAnalysis;
import gr.smaragda.dynamiclovtool.model.SearchField;
import gr.smaragda.dynamiclovtool.model.SearchFieldDropdownOption;
import gr.smaragda.dynamiclovtool.util.SessionUtil;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/api/search-fields/dropdown-options")
public class SearchFieldDropdownOptionController {
    @PostMapping("/add")
    public String addDropdownOption(@RequestParam String fieldDbParam,
                                    @ModelAttribute SearchFieldDropdownOption option,
                                    HttpSession session,
                                    Model model) {

        if (fieldDbParam == null || option.getLabel() == null || option.getValue() == null) {
            model.addAttribute("error", "Παρακαλώ συμπληρώστε label και value");
            return "redirect:/";
        }

        try {
            QueryAnalysis analysis = SessionUtil.getQueryAnalysisOrThrowException(session);

            Optional<SearchField> fieldOpt = analysis.getSearchFields().stream()
                    .filter(f -> f.getDbParam().equals(fieldDbParam))
                    .findFirst();

            if (fieldOpt.isPresent()) {
                fieldOpt.get().addDropdownOption(option.getLabel(), option.getValue());
                log.info("Added dropdown option to field {}: {} -> {}", fieldDbParam, option.getLabel(), option.getValue());
                model.addAttribute("success", "Dropdown option added successfully!");
            } else {
                model.addAttribute("error", "Field not found");
            }

            return "redirect:/";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/";
        }

    }

    @PostMapping("/update")
    @ResponseBody
    public String updateDropdownOption(@RequestParam String fieldDbParam,
                                       @RequestParam int optionIndex,
                                       @RequestParam(required = false) String label,
                                       @RequestParam(required = false) String value,
                                       HttpSession session) {

        try {
            QueryAnalysis analysis = SessionUtil.getQueryAnalysisOrThrowException(session);

            Optional<SearchField> fieldOpt = analysis.getSearchFields().stream()
                    .filter(f -> f.getDbParam().equals(fieldDbParam))
                    .findFirst();

            if (fieldOpt.isPresent()) {
                SearchField field = fieldOpt.get();
                if (optionIndex >= 0 && optionIndex < field.getDropdownOptions().size()) {
                    SearchFieldDropdownOption option = field.getDropdownOptions().get(optionIndex);

                    if (label != null && !label.trim().isEmpty()) option.setLabel(label);
                    if (value != null && !value.trim().isEmpty()) option.setValue(value);

                    log.info("Updated dropdown option at index {} for field {}: {} -> {}",
                            optionIndex, fieldDbParam, label, value);
                    return "OK";
                }
                return "Error: Invalid option index";
            }
            return "Error: Field not found";
        } catch (Exception e) {
            log.error("Error updating dropdown option for field {}", fieldDbParam, e);
            return "Error: " + e.getMessage();
        }
    }

    @PostMapping("/remove")
    @ResponseBody
    public String removeDropdownOption(@RequestParam String fieldDbParam,
                                       @RequestParam int optionIndex,
                                       HttpSession session) {
        try {
            QueryAnalysis analysis = SessionUtil.getQueryAnalysisOrThrowException(session);

            Optional<SearchField> fieldOpt = analysis.getSearchFields().stream()
                    .filter(f -> f.getDbParam().equals(fieldDbParam))
                    .findFirst();

            if (fieldOpt.isPresent()) {
                SearchField field = fieldOpt.get();
                if (optionIndex >= 0 && optionIndex < field.getDropdownOptions().size()) {
                    field.getDropdownOptions().remove(optionIndex);
                    log.info("Removed dropdown option at index {} from field {}", optionIndex, fieldDbParam);
                    return "OK";
                }
                return "Error: Invalid option index";
            }
            return "Error: Field not found";
        } catch (Exception e) {
            log.error("Error removing dropdown option from field {}", fieldDbParam, e);
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/{fieldDbParam}")
    public String showDropdownOptions(@PathVariable String fieldDbParam,
                                      Model model,
                                      HttpSession session) {

        try {
            QueryAnalysis analysis = SessionUtil.getQueryAnalysisOrThrowException(session);

            Optional<SearchField> fieldOpt = analysis.getSearchFields().stream()
                    .filter(f -> f.getDbParam().equals(fieldDbParam))
                    .findFirst();

            if (fieldOpt.isPresent()) {
                model.addAttribute("field", fieldOpt.get());
                return "fragments/dropdown-options-modal :: dropdownOptionsContent";
            }

            model.addAttribute("error", "Field not found");
            return "fragments/error :: errorContent";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/";
        }


    }
}
