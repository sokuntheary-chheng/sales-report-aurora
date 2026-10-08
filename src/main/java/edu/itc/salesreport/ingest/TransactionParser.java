package edu.itc.salesreport.ingest;

import edu.itc.salesreport.model.SaleTransaction;

/** Turns one CSV line into a SaleTransaction, or refuses it. */
public interface TransactionParser {

    /**
     * Parses a single line of the canonical export.
     *
     * @throws InvalidRowException if the line is malformed or breaks a rule
     */
    SaleTransaction parse(String line) throws InvalidRowException;

}
