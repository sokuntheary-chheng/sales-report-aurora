package edu.itc.salesreport.web;

import edu.itc.salesreport.engine.DefaultSalesAggregator;
import edu.itc.salesreport.engine.SalesAggregator;
import edu.itc.salesreport.ingest.CsvMonthReader;
import edu.itc.salesreport.ingest.CsvTransactionParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * The beans of the web page.
 *
 * <p>They come from the desktop project: the web page reuses the domain, the
 * parser and the aggregator of {@code edu.itc:sales-report} and adds no
 * business logic of its own.
 */
@Configuration
public class SalesReportConfiguration {

    @Bean
    CsvMonthReader csvMonthReader() {
        return new CsvMonthReader(new CsvTransactionParser());
    }

    @Bean
    SalesAggregator salesAggregator() {
        return new DefaultSalesAggregator();
    }

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
