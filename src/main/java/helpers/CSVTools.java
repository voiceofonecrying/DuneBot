package helpers;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

public class CSVTools {
    public static CSVFormat getFormat() {
        return CSVFormat.Builder.create()
                .setHeader()
                .setDelimiter(',')
                .setQuote('"')
                .setRecordSeparator("\r\n")
                .setIgnoreEmptyLines(false)
                .setAllowMissingColumnNames(true)
                .setAllowDuplicateHeaderNames(true)
                .build();
    }

    public static CSVParser getParser(String path) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(CSVTools.class.getClassLoader().getResourceAsStream(path))
        ));

        return CSVParser.parse(bufferedReader, getFormat());
    }
}
