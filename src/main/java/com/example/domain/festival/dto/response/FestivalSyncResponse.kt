package com.example.domain.festival.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FestivalSyncResponse {
    private int totalCount;
    private int createdCount;
    private int updatedCount;
    private int failedCount;
}
