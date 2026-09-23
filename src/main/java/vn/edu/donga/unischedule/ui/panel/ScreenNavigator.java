package vn.edu.donga.unischedule.ui.panel;

public interface ScreenNavigator {
    void showScreen(String screenKey);
    default void showReportYear(int year) {showScreen("reports");}
}
