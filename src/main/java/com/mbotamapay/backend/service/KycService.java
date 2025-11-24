package com.mbotamapay.backend.service;

import com.mbotamapay.backend.entity.KycDocument;
import com.mbotamapay.backend.entity.KycLevel;
import com.mbotamapay.backend.entity.User;
import com.mbotamapay.backend.repository.KycDocumentRepository;
import com.mbotamapay.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KycService {

    private final KycDocumentRepository kycDocumentRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    private static final String UPLOAD_DIR = "uploads/kyc/";

    @Transactional
    public KycDocument uploadDocument(User user, MultipartFile file, String documentTypeStr) throws IOException {
        // Ensure directory exists
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Save file
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        // Create record
        KycDocument document = KycDocument.builder()
                .user(user)
                .documentType(KycDocument.DocumentType.valueOf(documentTypeStr))
                .filePath(filePath.toString())
                .originalFilename(file.getOriginalFilename())
                .fileSize(file.getSize())
                .status(KycDocument.DocumentStatus.PENDING)
                .build();

        auditService.logAction(user.getEmail(), "KYC_UPLOAD", "Uploaded document: " + documentTypeStr, null);
        return kycDocumentRepository.save(document);
    }

    public List<KycDocument> getPendingDocuments() {
        return kycDocumentRepository.findByStatus(KycDocument.DocumentStatus.PENDING);
    }

    @Transactional
    public void verifyDocument(Long documentId, boolean approved, String adminEmail) {
        KycDocument document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        document.setStatus(approved ? KycDocument.DocumentStatus.VERIFIED : KycDocument.DocumentStatus.REJECTED);
        document.setVerifiedAt(LocalDateTime.now());
        kycDocumentRepository.save(document);

        if (approved) {
            User user = document.getUser();
            // Upgrade to LEVEL_1 if currently UNVERIFIED or null
            if (user.getKycLevel() == null || user.getKycLevel() == KycLevel.UNVERIFIED) {
                user.setKycLevel(KycLevel.LEVEL_1);
                userRepository.save(user);
            }
        }

        auditService.logAction(adminEmail, "KYC_VERIFY",
                "Document " + documentId + " " + (approved ? "APPROVED" : "REJECTED"), null);
    }
}
