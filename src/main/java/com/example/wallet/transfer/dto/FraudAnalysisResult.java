package com.example.wallet.transfer.dto;

import com.example.wallet.transfer.TransferStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class FraudAnalysisResult {

    private FraudRiskLevel riskLevel;

    private List<String> reasons;

    private BigDecimal suspiciousAmount;

    private String message;

    private UUID transferId;

    private TransferStatus transferStatus;
}
