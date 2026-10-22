package edu.itc.salesreport.web;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The page is tested without a server: {@link MockMvcTester} calls the
 * controller and checks the body of the answer.
 */
@WebMvcTest(controllers = ReportWebController.class,
        properties = "salesreport.data-dir=target/test-data")
@Import({SalesReportConfiguration.class, ReportWebControllerTest.FixedClock.class})
class ReportWebControllerTest {

    /** The clock of the test is fixed, so the test passes on every day. */
    @TestConfiguration
    static class FixedClock {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-09-27T03:00:00Z"),
                    ZoneId.of("Asia/Phnom_Penh"));
        }
    }

    @Autowired
    MockMvcTester mvc;

    @BeforeAll
    static void writeTestData() throws IOException {
        Path month = Files.createDirectories(Path.of("target/test-data/2026-09"));
        Files.writeString(month.resolve("PNH-2026-09-01.csv"), """
            branch,date,receipt_no,sku,product_name,category,quantity,unit_price,discount,payment_method
            PNH,2026-09-01,PNH-000001,SKU-1001,Jasmine Rice 5kg,Grocery,2,6.50,0.00,KHQR
            PNH,2026-09-01,PNH-000001,SKU-5002,Power Bank 10000mAh,Electronics,1,18.90,0.90,KHQR
            """);
        Files.writeString(month.resolve("REP-2026-09-01.csv"), """
            branch,date,receipt_no,sku,product_name,category,quantity,unit_price,discount,payment_method
            REP,2026-09-01,REP-000001,SKU-4003,Sunscreen SPF50,Personal Care,3,7.50,1.50,CASH
            """);
    }

    @Test
    void rendersSummaryOfOneBranch() {
        assertThat(mvc.get().uri("/report?month=2026-09&branch=PNH"))
                .hasStatusOk()
                .hasViewName("report")
                .bodyText().contains("Revenue USD 31.00", "Jasmine Rice 5kg",
                        "Power Bank 10000mAh");
    }

    @Test
    void anInvalidMonthBecomesAMessageAndNotAnError() {
        assertThat(mvc.get().uri("/report")
                        .param("month", "09/2026")
                        .param("branch", "PNH"))
                .hasStatusOk()
                .hasViewName("report")
                .bodyText().contains("is not a month in the form yyyy-MM");
    }

    @Test
    void anUnknownBranchBecomesAMessageAndNotAnError() {
        assertThat(mvc.get().uri("/report?month=2026-09&branch=XYZ"))
                .hasStatusOk()
                .hasViewName("report")
                .bodyText().contains("Unknown branch");
    }
}
