package com.mbotamapay.backend.repository;

import com.mbotamapay.backend.entity.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import com.mbotamapay.backend.entity.KycDocument.DocumentStatus;

@Repository
public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {
    List<KycDocument> findByStatus(DocumentStatus status);

    List<KycDocument> findByUserId(Long userId);
}
