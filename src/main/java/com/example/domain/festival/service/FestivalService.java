package com.example.domain.festival.service;

import com.example.domain.bookmark.repository.FestivalBookmarkRepository;
import com.example.domain.festival.dto.FestivalDetailResponseDto;
import com.example.domain.festival.dto.FestivalListResponseDto;
import com.example.domain.festival.dto.FestivalSearchRequestDto;
import com.example.domain.festival.entity.Festival;
import com.example.domain.festival.entity.FestivalStatus;
import com.example.domain.festival.repository.FestivalRepository;
import com.example.domain.member.entity.Member;
import com.example.domain.member.repository.MemberRepository;
import com.example.global.exception.CustomNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@AllArgsConstructor
public class FestivalService {

    private final FestivalRepository festivalRepository;
    private final MemberRepository memberRepository;
    private final FestivalBookmarkRepository festivalBookmarkRepository;

    public Page<Festival> searchFestivals(FestivalSearchRequestDto searchDto, Pageable pageable) {
        return festivalRepository.searchFestivals(searchDto, pageable);
    }

    // 로그인 유저의 찜 여부까지 채워서 목록 DTO 페이지로 반환
    public Page<FestivalListResponseDto> searchFestivalsDto(
            FestivalSearchRequestDto searchDto, Pageable pageable, String loginId) {
        Page<Festival> page = festivalRepository.searchFestivals(searchDto, pageable);

        Set<Long> bookmarkedIds = resolveBookmarkedIds(loginId, page.getContent());

        return page.map(f -> FestivalListResponseDto.from(f, bookmarkedIds.contains(f.getId())));
    }

    @Transactional
    public Festival getFestival(Long id) {
        festivalRepository.incrementViewCount(id);

        return festivalRepository.findById(id)
                .orElseThrow(()-> new CustomNotFoundException("404","존재하지 않는 축제입니다."));
    }

    // 로그인 유저의 찜 여부까지 채워서 상세 DTO로 반환
    @Transactional
    public FestivalDetailResponseDto getFestivalDetail(Long id, String loginId) {
        Festival festival = getFestival(id);
        boolean isBookmarked = false;

        if (loginId != null) {
            Long memberId = memberRepository.findByLoginId(loginId)
                    .map(Member::getId)
                    .orElse(null);
            if (memberId != null) {
                isBookmarked = festivalBookmarkRepository
                        .existsByMemberIdAndFestivalId(memberId, id);
            }
        }

        return FestivalDetailResponseDto.from(festival, isBookmarked);
    }

    public List<Festival> getNearbyMarkers(FestivalSearchRequestDto searchDto){
        return festivalRepository.findNearbyFestivals(searchDto);
    }

    private Set<Long> resolveBookmarkedIds(String loginId, List<Festival> festivals) {
        if (loginId == null || festivals.isEmpty()) {
            return Set.of();
        }

        Long memberId = memberRepository.findByLoginId(loginId)
                .map(Member::getId)
                .orElse(null);
        if (memberId == null) {
            return Set.of();
        }

        List<Long> festivalIds = festivals.stream().map(Festival::getId).toList();
        return new HashSet<>(
                festivalBookmarkRepository.findBookmarkedFestivalIds(memberId, festivalIds)
        );
    }
}
