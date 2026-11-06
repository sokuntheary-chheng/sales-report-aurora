package edu.itc.salesreport.ingest;

import edu.itc.salesreport.model.SaleTransaction;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Reads the branch files of one month through the canonical parser.
 *
 * <p>The files of a month live in {@code dataDir/yyyy-MM/} and are named
 * {@code <BRANCH>-yyyy-MM-dd.csv}. They are read in name order, the header
 * line of every file is skipped and a row that the parser refuses is
 * reported instead of being summed.
 */
public final class CsvMonthReader {

    /** The branch files of a month. */
    private static final Pattern BRANCH_FILE =
            Pattern.compile("(PNH|REP|BTB)-\\d{4}-\\d{2}-\\d{2}\\.csv");

    private final TransactionParser parser;

    public CsvMonthReader(TransactionParser parser) {
        this.parser = parser;
    }

    /**
     * Reads every branch file of one month.
     *
     * @param dataDir the folder that holds the month folders
     * @param month   the month to read
     * @return the accepted rows and the refused rows of the month
     * @throws NoSuchFileException if the folder of the month does not exist
     * @throws IOException          if a file cannot be read
     */
    public MonthData read(Path dataDir, YearMonth month) throws IOException {
        return read(dataDir, month, null);
    }

    /**
     * Reads the branch files of one month for a single branch, so a
     * per-branch load does not parse the files of the other branches
     * again for every branch it reports.
     *
     * @param dataDir the folder that holds the month folders
     * @param month   the month to read
     * @param branch  the branch whose files are read (PNH, REP or BTB)
     * @return the accepted rows and the refused rows of that branch
     * @throws NoSuchFileException if the folder of the month does not exist
     * @throws IOException          if a file cannot be read
     */
    public MonthData readBranch(Path dataDir, YearMonth month, String branch)
            throws IOException {
        return read(dataDir, month, Objects.requireNonNull(branch));
    }

    private MonthData read(Path dataDir, YearMonth month, String branch)
            throws IOException {
        Path dir = dataDir.resolve(month.toString());
        if (!Files.isDirectory(dir)) {
            throw new NoSuchFileException(dir.toString());
        }
        var rows = new ArrayList<SaleTransaction>();
        var rejected = new ArrayList<String>();
        for (Path file : filesOf(dir, branch)) {
            var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (int line = 1; line < lines.size(); line++) {
                try {
                    rows.add(parser.parse(lines.get(line)));
                } catch (InvalidRowException e) {
                    rejected.add(file.getFileName() + ":" + (line + 1)
                            + " " + e.getMessage());
                }
            }
        }
        return new MonthData(month, rows, rejected);
    }

    /**
     * The branch files of a month, sorted by name; with a branch, only
     * that branch's files.
     */
    private static List<Path> filesOf(Path dir, String branch)
            throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> {
                        String name = p.getFileName().toString();
                        return BRANCH_FILE.matcher(name).matches()
                                && (branch == null
                                || name.startsWith(branch + "-"));
                    })
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        }
    }
}
