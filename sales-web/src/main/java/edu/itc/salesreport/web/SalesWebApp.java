package edu.itc.salesreport.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * The web page of the Angkor Mart monthly sales report.
 *
 * <p>Nothing of the domain is repeated here: the page asks the same model
 * for the same figures as the desktop viewer.
 */
@SpringBootApplication
@Import(SalesReportConfiguration.class)
public class SalesWebApp {

    public static void main(String[] args) {
        SpringApplication.run(SalesWebApp.class, args);
    }
}
