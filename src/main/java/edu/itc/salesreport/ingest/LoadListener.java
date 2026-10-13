package edu.itc.salesreport.ingest;

/** An observer of a load run; the UI and the notifier subscribe to it. */
@FunctionalInterface
public interface LoadListener {

    void onEvent(LoadEvent event);

}
