package com.example.domain.festival.controller;

import com.example.domain.festival.dto.*;
import com.example.domain.festival.entity.Festival;
import com.example.domain.festival.service.FestivalService;
import com.example.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/festivals")
@Tag(name = "Festival", description = "사용자 축제 정보 조회 API")
public class FestivalController {

    private final FestivalService festivalService;

    @GetMapping
    @Operation(summary = "축제 목록 조회",
            description = "지역, 상태, 월, 키워드 등으로 축제를 검색하고 목록을 반환합니다. 로그인 시 찜 여부(isBookmarked)가 포함됩니다.")
    public ResponseEntity<RsData<FestivalPageResponseDto<FestivalListResponseDto>>> searchFestivals(
            @ParameterObject @ModelAttribute FestivalSearchRequestDto searchDto,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable,
            Authentication authentication){

        String loginId = resolveLoginId(authentication);

        Page<FestivalListResponseDto> dtopage =
                festivalService.searchFestivalsDto(searchDto, pageable, loginId);

        FestivalPageResponseDto<FestivalListResponseDto> pageData =
                FestivalPageResponseDto.from(dtopage);

        return ResponseEntity.ok(RsData.success("축제 목록 조회 성공", pageData));
    }

    @GetMapping("/{id}")
    @Operation(summary = "축제 상세 조회",
            description = "특정 축제의 상세 정보를 조회합니다. 조회 시 viewCount가 1 증가합니다. 로그인 시 찜 여부(isBookmarked)가 포함됩니다.")
    public ResponseEntity<RsData<FestivalDetailResponseDto>> getFestivalDetail(
            @PathVariable Long id,
            Authentication authentication
    ){
        String loginId = resolveLoginId(authentication);
        FestivalDetailResponseDto responseDto = festivalService.getFestivalDetail(id, loginId);

        return ResponseEntity.ok(new RsData<>("200", "축제 상세 조회 성공", responseDto));
    }

    @GetMapping("/nearby")
    @Operation(summary = "위치기반 주변 축제 검색",
            description = "내 위치(mapX, mapY) 기준으로 반경 내의 축제 목록을 반환합니다.")
    public ResponseEntity<RsData<List<FestivalMarkerResponseDto>>> getNearbyFestivals(
            @ParameterObject @ModelAttribute FestivalSearchRequestDto searchDto
    ){
        FestivalSearchRequestDto mapSearchDto = searchDto.applyMapDefaults();
        List<Festival> festivals = festivalService.getNearbyMarkers(mapSearchDto);

        List<FestivalMarkerResponseDto> markerDtoList = festivals.stream()
                .map(FestivalMarkerResponseDto::from)
                .toList();

        RsData<List<FestivalMarkerResponseDto>> rsData = new RsData<>("200", "주변 축제 조회 성공", markerDtoList);
        return ResponseEntity.ok(rsData);
    }

    // 비로그인 요청이면 null을 돌려주어 서비스가 찜 여부를 false로 처리하도록 한다.
    private String resolveLoginId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String name = authentication.getName();
        if (name == null || "anonymousUser".equals(name)) {
            return null;
        }
        return name;
    }
}
