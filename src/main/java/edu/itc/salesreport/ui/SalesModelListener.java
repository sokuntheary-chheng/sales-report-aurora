package edu.itc.salesreport.ui;

/** Notified after every change of the model. */
@FunctionalInterface
public interface SalesModelListener {

    /** Called on the thread that changed the model. */
    void modelChanged(SalesEvent event);
}
