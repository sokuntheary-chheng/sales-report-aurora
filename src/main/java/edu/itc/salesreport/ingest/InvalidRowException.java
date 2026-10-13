package edu.itc.salesreport.ingest;

/** A row that could not be parsed; the message names the offending field. */
public class InvalidRowException extends Exception {

    public InvalidRowException(String message) {
        super(message);
    }

    public InvalidRowException(String message, Throwable cause) {
        super(message, cause);
    }

}
