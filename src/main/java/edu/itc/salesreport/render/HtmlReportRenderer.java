package edu.itc.salesreport.render;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.MonthlyReport;

import java.nio.charset.StandardCharsets;

/** HTML strategy: one table row per branch plus the chain. */
public final class HtmlReportRenderer implements ReportRenderer {

    @Override
    public String extension() {
        return "html";
    }

    @Override
    public byte[] render(MonthlyReport report) {
        var title = "Angkor Mart monthly sales " + report.month();
        var sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"utf-8\"/>\n")
          .append("<title>").append(escape(title)).append("</title>\n")
          .append("</head>\n<body>\n")
          .append("<h1>").append(escape(title)).append("</h1>\n")
          .append("<table>\n<thead>\n<tr>")
          .append("<th>Branch</th><th>Revenue</th>")
          .append("<th>Receipts</th><th>Avg basket</th>")
          .append("</tr>\n</thead>\n<tbody>\n");
        for (BranchSummary b : report.branches()) {
            row(sb, b);
        }
        row(sb, report.chain());
        sb.append("</tbody>\n</table>\n</body>\n</html>\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void row(StringBuilder sb, BranchSummary b) {
        sb.append("<tr><td>").append(escape(b.branch()))
          .append("</td><td>").append(escape(b.revenue().toPlainString()))
          .append("</td><td>").append(b.receipts())
          .append("</td><td>").append(escape(b.averageBasket().toPlainString()))
          .append("</td></tr>\n");
    }

    /** Escapes & first, then &lt; and &gt;, so no entity is double-escaped. */
    static String escape(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

}
