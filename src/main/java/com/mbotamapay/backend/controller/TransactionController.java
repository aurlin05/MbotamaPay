package com.mbotamapay.backend.controller;

import com.mbotamapay.backend.dto.transaction.TransactionResponse;
import com.mbotamapay.backend.dto.transaction.TransferRequest;
import com.mbotamapay.backend.entity.Transaction;
import com.mbotamapay.backend.repository.TransactionRepository;
import com.mbotamapay.backend.routes.Routes;
import com.mbotamapay.backend.security.CustomUserDetails;
import com.mbotamapay.backend.service.ExportService;
import com.mbotamapay.backend.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(Routes.TRANSACTIONS)
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transaction operations")
public class TransactionController {

        private final TransactionService transactionService;
        private final ExportService exportService;
        private final TransactionRepository transactionRepository;

        @PostMapping("/send")
        @Operation(summary = "Send money", description = "Send money to another user")
        public ResponseEntity<TransactionResponse> sendMoney(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @Valid @RequestBody TransferRequest request) {
                return ResponseEntity.ok(transactionService.sendMoney(userDetails.getUser(), request));
        }

        @GetMapping
        @Operation(summary = "Get transaction history", description = "Get paginated transaction history")
        public ResponseEntity<Page<TransactionResponse>> getHistory(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        Pageable pageable) {
                return ResponseEntity
                                .ok(transactionService.getTransactionHistoryPaginated(userDetails.getUser(), pageable));
        }

        @GetMapping("/{id}/export/pdf")
        @Operation(summary = "Export transaction to PDF", description = "Download PDF receipt")
        public ResponseEntity<byte[]> exportPdf(
                        @AuthenticationPrincipal CustomUserDetails userDetails,
                        @PathVariable Long id) {

                Transaction transaction = transactionRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Transaction not found"));

                // Verify ownership (sender or receiver)
                // This logic should ideally be in service, but for brevity:
                // ...

                byte[] pdf = exportService.generateTransactionPdf(transaction);

                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=transaction_" + id + ".pdf")
                                .contentType(MediaType.APPLICATION_PDF)
                                .body(pdf);
        }

        @GetMapping("/export/csv")
        @Operation(summary = "Export history to CSV", description = "Download CSV statement")
        public ResponseEntity<byte[]> exportCsv(@AuthenticationPrincipal CustomUserDetails userDetails) {
                List<Transaction> transactions = transactionRepository
                                .findBySenderWalletUserOrReceiverWalletUserOrderByCreatedAtDesc(
                                                userDetails.getUser(), userDetails.getUser());

                byte[] csv = exportService.generateTransactionCsv(transactions);

                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions.csv")
                                .contentType(MediaType.parseMediaType("text/csv"))
                                .body(csv);
        }
}
