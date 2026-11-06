package edu.itc.salesreport.ui;

import javafx.scene.Scene;
import javafx.stage.Stage;

/** Challenge wiring: a second window bound to the same ViewModel. */
final class DashboardWindow {

    /** Opens the dashboard on the ViewModel the viewer already uses. */
    static Stage open(ReportViewModel vm) {
        Stage dash = new Stage();
        dash.setScene(new Scene(new DashboardView(vm).root(), 320, 260));
        dash.setTitle("Angkor Mart · Dashboard");
        dash.setAlwaysOnTop(true);   // the operations manager's window
        dash.show();
        return dash;
    }

    private DashboardWindow() { }
}
