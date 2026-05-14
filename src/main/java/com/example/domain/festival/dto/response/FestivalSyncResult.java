package com.example.domain.festival.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

//동기화 결과 반환용 DTO
@Getter
@AllArgsConstructor
public class FestivalSyncResult {
    private int totalCount;
    private int createdCount;
    private int updatedCount;
    private int failedCount;
    private List<String> changedContentIds;
}
