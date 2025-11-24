package com.mbotamapay.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "kyc_documents")
public class KycDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String originalFilename;

    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime verifiedAt;

    public enum DocumentType {
        NATIONAL_ID,
        PASSPORT,
        DRIVERS_LICENSE,
        PROOF_OF_ADDRESS
    }

    public enum DocumentStatus {
        PENDING,
        VERIFIED,
        REJECTED
    }
}
