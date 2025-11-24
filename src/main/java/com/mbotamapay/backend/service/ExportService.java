package com.mbotamapay.backend.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.mbotamapay.backend.entity.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] generateTransactionPdf(Transaction transaction) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Transaction Receipt", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);

            addTableCell(table, "Reference:", transaction.getReference());
            addTableCell(table, "Date:", transaction.getCreatedAt().format(DATE_FORMATTER));
            addTableCell(table, "Type:", transaction.getType().name());
            addTableCell(table, "Status:", transaction.getStatus().name());
            addTableCell(table, "Amount:", transaction.getAmount() + " XAF");
            addTableCell(table, "Fee:", transaction.getFee() + " XAF");

            String sender = transaction.getSenderWallet() != null ? transaction.getSenderWallet().getUser().getEmail()
                    : "System";
            String receiver = transaction.getReceiverWallet() != null
                    ? transaction.getReceiverWallet().getUser().getEmail()
                    : "System";

            addTableCell(table, "Sender:", sender);
            addTableCell(table, "Receiver:", receiver);
            addTableCell(table, "Description:", transaction.getDescription());

            document.add(table);
            document.close();

            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private void addTableCell(PdfPTable table, String header, String value) {
        PdfPCell headerCell = new PdfPCell(new Phrase(header));
        headerCell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
        table.addCell(headerCell);
        table.addCell(value != null ? value : "");
    }

    public byte[] generateTransactionCsv(List<Transaction> transactions) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                PrintWriter writer = new PrintWriter(out)) {

            // Header
            writer.println("ID,Reference,Date,Type,Status,Amount,Fee,Sender,Receiver,Description");

            for (Transaction tx : transactions) {
                String sender = tx.getSenderWallet() != null ? tx.getSenderWallet().getUser().getEmail() : "System";
                String receiver = tx.getReceiverWallet() != null ? tx.getReceiverWallet().getUser().getEmail()
                        : "System";

                writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        tx.getId(),
                        escapeCsv(tx.getReference()),
                        tx.getCreatedAt().format(DATE_FORMATTER),
                        tx.getType(),
                        tx.getStatus(),
                        tx.getAmount(),
                        tx.getFee(),
                        escapeCsv(sender),
                        escapeCsv(receiver),
                        escapeCsv(tx.getDescription()));
            }

            writer.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CSV", e);
        }
    }

    private String escapeCsv(String value) {
        if (value == null)
            return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
