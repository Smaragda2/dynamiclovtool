package gr.smaragda.dynamiclovtool.controller;

import gr.smaragda.dynamiclovtool.model.QueryAnalysis;
import gr.smaragda.dynamiclovtool.model.ResultColumn;
import gr.smaragda.dynamiclovtool.util.SessionUtil;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/api/result-columns")
public class ResultColumnController {
    @PostMapping("/update")
    @ResponseBody
    public String updateColumn(@RequestParam String nameInResults,
                               @RequestParam(required = false) String title,
                               @RequestParam(required = false) Boolean visible,
                               HttpSession session) {

        try {
            QueryAnalysis analysis = SessionUtil.getQueryAnalysisOrThrowException(session);

            Optional<ResultColumn> columnOpt = analysis.getResultColumns().stream()
                    .filter(c -> c.getNameInResults().equals(nameInResults))
                    .findFirst();

            if (columnOpt.isPresent()) {
                ResultColumn column = columnOpt.get();
                if (title != null && !title.trim().isEmpty()) column.setTitle(title);
                if (visible != null) column.setVisible(visible);

                log.info("Updated column {}: title={}, visible={}", nameInResults, title, visible);
                return "OK";
            }
            return "Error: Column not found";
        } catch (Exception e) {
            log.error("Error updating column {}", nameInResults, e);
            return "Error: " + e.getMessage();
        }
    }
}
