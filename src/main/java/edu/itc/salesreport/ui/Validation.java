package edu.itc.salesreport.ui;

/**
 * The result of an input rule: a value, or the message that explains why the
 * input was refused.
 *
 * @param <T> the type of the value
 */
public sealed interface Validation<T> {

    /**
     * The input was good.
     *
     * @param value the value the rule found
     */
    record Valid<T>(T value) implements Validation<T> {
    }

    /**
     * The input was refused.
     *
     * @param message the message the user gets to see
     */
    record Invalid<T>(String message) implements Validation<T> {
    }
}
