package gr.smaragda.dynamiclovtool.util;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

public class FacesContextUtil {
    public static void addErrorMessage(String message) {
        FacesContext
                .getCurrentInstance()
                .addMessage(null
                        , new FacesMessage(
                                FacesMessage.SEVERITY_ERROR
                                , "Σημαντική ειδοποίηση: " + message
                                , ""
                        )
                );
    }

    public static void addWarnMessage(String message) {
        FacesContext
                .getCurrentInstance()
                .addMessage(null
                        , new FacesMessage(
                                FacesMessage.SEVERITY_WARN
                                , "Προειδοποίηση: " + message
                                , ""
                        )
                );
    }

    public static void addInfoMessage(String message) {
        FacesContext
                .getCurrentInstance()
                .addMessage(null
                        , new FacesMessage(
                                FacesMessage.SEVERITY_INFO
                                , "Σημείωση: " + message
                                , ""
                        )
                );
    }

    public static void addAlertMessage(String message) {
        if (message.equalsIgnoreCase("-150003: non-ORACLE exception")) {
            return;
        }
        FacesContext
                .getCurrentInstance()
                .addMessage(null
                        , new FacesMessage(
                                FacesMessage.SEVERITY_WARN
                                , "Προσοχή: " + message
                                , ""
                        )
                );
    }

    public static void addSuccessMessage(String message) {
        FacesContext
                .getCurrentInstance()
                .addMessage(null
                        , new FacesMessage(
                                FacesMessage.SEVERITY_INFO
                                , message
                                , ""
                        )
                );
    }

    public static void showAlerts(String... alerts) {
        for (String alert : alerts) {
            if (alert != null)  {
                addAlertMessage(alert);
            }
        }
    }
}
