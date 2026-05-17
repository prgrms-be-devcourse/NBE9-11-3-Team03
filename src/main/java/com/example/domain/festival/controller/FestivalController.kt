package com.example.domain.festival.controller

import com.example.domain.festival.dto.request.FestivalSearchRequest
import com.example.domain.festival.dto.response.FestivalDetailResponse
import com.example.domain.festival.dto.response.FestivalListResponse
import com.example.domain.festival.dto.response.FestivalMarkerResponse
import com.example.domain.festival.dto.response.FestivalPageResponse
import com.example.domain.festival.service.FestivalService
import com.example.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/festivals")
@Tag(name = "Festival", description = "사용자 축제 정보 조회 API")
class FestivalController(
    private val festivalService: FestivalService,
) {


    @GetMapping
    @Operation(
        summary = "축제 목록 조회",
        description = "지역, 상태, 월, 키워드 등으로 축제를 검색하고 목록을 반환합니다. 로그인 시 찜 여부(isBookmarked)가 포함됩니다."
    )
    fun searchFestivals(
        @ParameterObject @ModelAttribute searchDto: FestivalSearchRequest,
        @ParameterObject @PageableDefault(size = 10) pageable: Pageable,
        authentication: Authentication?
    ): ResponseEntity<RsData<FestivalPageResponse<FestivalListResponse>>> = ResponseEntity.ok(
        RsData.success(
            "축제 목록 조회 성공",
            FestivalPageResponse.from(
                festivalService.searchFestivalsDto(searchDto, pageable, resolveLoginId(authentication))

            )
        )
    )

    @GetMapping("/{id}")
    @Operation(
        summary = "축제 상세 조회",
        description = "특정 축제의 상세 정보를 조회합니다. 조회 시 viewCount가 1 증가합니다. 로그인 시 찜 여부(isBookmarked)가 포함됩니다."
    )
    fun getFestivalDetail(
        @PathVariable id: Long,
        authentication: Authentication?
    ): ResponseEntity<RsData<FestivalDetailResponse>> = ResponseEntity.ok(
        RsData.success(
            "축제 상세 조회 성공",
            festivalService.getFestivalDetail(id, resolveLoginId(authentication))
        )
    )


    @GetMapping("/nearby")
    @Operation(summary = "위치기반 주변 축제 검색", description = "내 위치(mapX, mapY) 기준으로 반경 내의 축제 목록을 반환합니다.")
    fun getNearbyFestivals(
        @ParameterObject @ModelAttribute searchDto: FestivalSearchRequest
    ): ResponseEntity<RsData<List<FestivalMarkerResponse>>> = ResponseEntity.ok(
        RsData.success(
            "주변 축제 조회 성공",
            festivalService.getNearbyMarkers(searchDto.applyMapDefaults())
                .map(FestivalMarkerResponse::from)
        )
    )


    // 비로그인 요청이면 null을 돌려주어 서비스가 찜 여부를 false로 처리하도록 한다.
    private fun resolveLoginId(authentication: Authentication?): String? =
        authentication
            ?.takeIf { it.isAuthenticated }    // 인증된 사용자만 처리
            ?.name                             // 로그인한 사용자의 ID (예: "user123")
            ?.takeUnless { it == "anonymousUser" } // 익명 사용자가 아닌 경우만 통과
}
