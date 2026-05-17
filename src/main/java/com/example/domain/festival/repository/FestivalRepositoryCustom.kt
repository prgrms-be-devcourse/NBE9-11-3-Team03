package com.example.domain.festival.repository

import com.example.domain.festival.dto.request.FestivalSearchRequest
import com.example.domain.festival.entity.Festival
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface FestivalRepositoryCustom {
    fun searchFestivals(searchDto: FestivalSearchRequest, pageable: Pageable): Page<Festival>
    fun findNearbyFestivals(searchDto: FestivalSearchRequest): List<Festival>
}
