package edu.itc.salesreport.ingest;

import edu.itc.salesreport.model.SaleTransaction;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/** Observer subject: loads one month of CSV files and reports progress. */
public final class MonthLoader {

    private final TransactionParser parser;
    private final List<LoadListener> listeners =
            new CopyOnWriteArrayList<>();
    private final List<String> rejectedRows = new ArrayList<>();

    public MonthLoader(TransactionParser parser) {
        this.parser = parser;
    }

    /** Registers a listener; the returned handle unsubscribes it. */
    public Runnable subscribe(LoadListener listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    /** Rejected rows of the last load as "FILE:LINE message". */
    public List<String> rejectedRows() {
        return List.copyOf(rejectedRows);
    }

    /**
     * Reads dataDir/&lt;month&gt;/BRANCH-*.csv in name order, skips the header
     * line of every file and parses the rest.
     */
    public List<SaleTransaction> load(YearMonth month, Path dataDir) {
        long start = System.nanoTime();
        rejectedRows.clear();
        var transactions = new ArrayList<SaleTransaction>();
        var files = csvFilesIn(dataDir.resolve(month.toString()));

        int done = 0;
        int acceptedTotal = 0;
        int rejectedTotal = 0;
        for (Path file : files) {
            done++;
            int accepted = 0;
            int rejected = 0;
            try {
                var lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                for (int i = 1; i < lines.size(); i++) {
                    try {
                        transactions.add(parser.parse(lines.get(i)));
                        accepted++;
                    } catch (InvalidRowException e) {
                        rejected++;
                        rejectedRows.add(file.getFileName() + ":" + (i + 1)
                                + " " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException("cannot read " + file, e);
            }
            acceptedTotal += accepted;
            rejectedTotal += rejected;
            publish(new LoadEvent.FileLoaded(file, accepted, rejected,
                    done, files.size()));
        }

        var took = Duration.ofNanos(System.nanoTime() - start);
        publish(new LoadEvent.LoadFinished(acceptedTotal, rejectedTotal, took));
        return List.copyOf(transactions);
    }

    private static List<Path> csvFilesIn(Path dir) {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> p.getFileName().toString().endsWith(".csv"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("cannot list " + dir, e);
        }
    }

    private void publish(LoadEvent event) {
        listeners.forEach(l -> l.onEvent(event));
    }

}
