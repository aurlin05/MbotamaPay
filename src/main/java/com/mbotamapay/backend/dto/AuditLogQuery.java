package com.mbotamapay.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogQuery {
    private Long userId;
    private String actionType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String severity;
}
