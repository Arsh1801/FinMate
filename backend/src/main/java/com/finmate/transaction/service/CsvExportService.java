package com.finmate.transaction.service;

import com.finmate.transaction.entity.Tag;
import com.finmate.transaction.entity.Transaction;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CsvExportService {

    public String generateCsv(List<Transaction> transactions) {
        StringWriter writer = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT
                .builder()
                .setHeader("ID", "Date", "Description", "Type", "Category", "Amount", "Payment Method", "Notes", "Tags")
                .build())) {

            for (Transaction t : transactions) {
                String tags = t.getTags() != null ?
                        t.getTags().stream().map(Tag::getName).collect(Collectors.joining("; ")) : "";

                printer.printRecord(
                        t.getId(),
                        t.getTransactionDate(),
                        t.getDescription(),
                        t.getType(),
                        t.getCategory() != null ? t.getCategory().getName() : "",
                        t.getAmount() != null ? t.getAmount().toPlainString() : "0.00",
                        t.getPaymentMethod(),
                        t.getNotes() != null ? t.getNotes() : "",
                        tags
                );
            }
            printer.flush();
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate CSV export", e);
        }
    }
}
