package com.example.domain.festival.repository;

import com.example.domain.festival.dto.FestivalSearchRequestDto;
import com.example.domain.festival.entity.Festival;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FestivalRepositoryCustom {
    Page<Festival> searchFestivals(FestivalSearchRequestDto searchDto, Pageable pageable);
    List<Festival> findNearbyFestivals(FestivalSearchRequestDto searchDto);
}
