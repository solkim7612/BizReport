package com.bizreport.core.dto.business;

import com.bizreport.core.entity.history.RefreshHistory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RefreshHistoryResponse {
    private String type;
    private int amount;
    private int balance;
    private LocalDateTime createdAt;

    public static RefreshHistoryResponse from(RefreshHistory history) {
        return RefreshHistoryResponse.builder()
                .type(history.getType())
                .amount(history.getAmount())
                .balance(history.getBalance())
                .createdAt(history.getCreatedAt())
                .build();
    }
}