package gr.smaragda.dynamiclovtool.util;

import gr.smaragda.dynamiclovtool.exception.QueryAnalysisNotFound;
import gr.smaragda.dynamiclovtool.model.QueryAnalysis;
import jakarta.servlet.http.HttpSession;

public class SessionUtil {
    public static QueryAnalysis getQueryAnalysisOrThrowException(HttpSession session) throws QueryAnalysisNotFound {
        QueryAnalysis analysis = (QueryAnalysis) session.getAttribute("analysis");
        if (analysis == null) {
            throw new QueryAnalysisNotFound("Παρακαλώ αναλύστε πρώτα ένα query");
        }

        return analysis;
    }

    public static QueryAnalysis getQueryAnalysisOrNull(HttpSession session) {
        return  (QueryAnalysis) session.getAttribute("analysis");
    }
}
