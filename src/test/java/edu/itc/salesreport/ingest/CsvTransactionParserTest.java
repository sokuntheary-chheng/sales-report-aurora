package edu.itc.salesreport.ingest;

import edu.itc.salesreport.model.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CsvTransactionParserTest {

    private final TransactionParser parser = new CsvTransactionParser();

    @Test
    void parsesAValidRow() throws InvalidRowException {
        var t = parser.parse("PNH,2026-09-01,PNH-000123,"
                + "SKU-1001,Jasmine Rice 5kg,Grocery,2,6.50,0.00,KHQR");
        assertEquals("PNH", t.branch());
        assertEquals(LocalDate.of(2026, 9, 1), t.date());
        assertEquals("PNH-000123", t.receiptNo());
        assertEquals("SKU-1001", t.sku());
        assertEquals("Jasmine Rice 5kg", t.productName());
        assertEquals("Grocery", t.category());
        assertEquals(2, t.quantity());
        assertEquals(new BigDecimal("6.50"), t.unitPrice());
        assertEquals(new BigDecimal("0.00"), t.discount());
        assertEquals(PaymentMethod.KHQR, t.paymentMethod());
        assertEquals(new BigDecimal("13.00"), t.revenue());
    }

    @Test
    void parsesRowWithDiscount() throws InvalidRowException {
        var t = parser.parse("REP,2026-09-02,REP-000007,"
                + "SKU-2002,Iced Coffee Can,Beverages,3,0.90,0.10,CARD");
        assertEquals(new BigDecimal("0.10"), t.discount());
        assertEquals(PaymentMethod.CARD, t.paymentMethod());
        assertEquals(new BigDecimal("2.60"), t.revenue());
    }

    @Test
    void stripsWhitespaceAroundEveryField() throws InvalidRowException {
        var t = parser.parse(" BTB , 2026-09-03 , BTB-000042 , SKU-6001 , "
                + " Notebook A5 , Stationery , 1 , 0.75 , 0.00 , CASH ");
        assertEquals("BTB", t.branch());
        assertEquals(LocalDate.of(2026, 9, 3), t.date());
        assertEquals("Stationery", t.category());
        assertEquals(new BigDecimal("0.75"), t.unitPrice());
        assertEquals(new BigDecimal("0.75"), t.revenue());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "PNH,2026-09-01,PNH-1,SKU-1,Rice,Grocery,2,6.50,0.00",
        "XYZ,2026-09-01,PNH-1,SKU-1,Rice,Grocery,2,6.50,0.00,CASH",
        "PNH,2026-09-31,PNH-1,SKU-1,Rice,Grocery,2,6.50,0.00,CASH",
        "PNH,2026-09-01,PNH-1,SKU-1,Rice,Toys,2,6.50,0.00,CASH",
        "PNH,2026-09-01,PNH-1,SKU-1,Rice,Grocery,two,6.50,0.00,CASH",
        "PNH,2026-09-01,PNH-1,SKU-1,Rice,Grocery,0,6.50,0.00,CASH",
        "PNH,2026-09-01,PNH-1,SKU-1,Rice,Grocery,2,6.50,-0.10,CASH",
        "PNH,2026-09-01,PNH-1,SKU-1,Rice,Grocery,2,6.50,20.00,CASH",
        "PNH,2026-09-01,PNH-1,SKU-1,Rice,Grocery,2,6.50,0.00,PAYPAL"
    })
    void rejectsInvalidRows(String line) {
        var thrown = assertThrows(InvalidRowException.class,
                () -> parser.parse(line));
        assertNotNull(thrown.getMessage());
        assertFalse(thrown.getMessage().isBlank());
    }

}
