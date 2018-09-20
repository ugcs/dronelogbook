package ugcs.csv;

import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.ugcs.common.util.Strings.isNullOrEmpty;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.joining;

public class CsvWriter {
    private final static String CSV_SEPARATOR = ",";

    private final PrintWriter writer;
    private final List<String> columnNames;
    private final Map<String, String> currentCsvRecord;

    public CsvWriter(List<String> columnNames, OutputStream out, Charset charset) {
        this.columnNames = unmodifiableList(columnNames);
        writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(out, charset)));
        currentCsvRecord = new HashMap<>();
        this.columnNames.forEach(columnName -> currentCsvRecord.put(columnName, ""));
    }

    public void printHeader(Function<String, String> columnNameMapper) {
        writer.println(columnNames.stream()
                .map(columnNameMapper)
                .collect(Collectors.joining(CSV_SEPARATOR))
        );
    }

    protected void printRecord(Function<String, String> columnNameToValueFunction) {
        columnNames.stream()
                .map(colName -> Pair.of(colName, columnNameToValueFunction.apply(colName)))
                .forEach(pair -> {
                    if (!isNullOrEmpty(pair.getRight())) {
                        currentCsvRecord.put(pair.getLeft(), pair.getRight());
                    }
                });

        writer.println(columnNames.stream()
                .map(currentCsvRecord::get)
                .collect(joining(CSV_SEPARATOR)));

        writer.flush();
    }
}
