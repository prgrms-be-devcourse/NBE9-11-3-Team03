package com.example.domain.bookmark.service;

import com.example.domain.bookmark.dto.FestivalBookmarkResponseDto;
import com.example.domain.bookmark.entity.FestivalBookmark;
import com.example.domain.bookmark.repository.FestivalBookmarkRepository;
import com.example.domain.festival.entity.Festival;
import com.example.domain.festival.repository.FestivalRepository;
import com.example.domain.member.entity.Member;
import com.example.domain.member.repository.MemberRepository;
import com.example.global.exception.BadRequestException;
import com.example.global.exception.ConflictException;
import com.example.global.exception.CustomNotFoundException;
import com.example.global.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FestivalBookmarkService {

    private final FestivalBookmarkRepository festivalBookmarkRepository;
    private final FestivalRepository festivalRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public FestivalBookmarkResponseDto bookmarkFestival(Long festivalId, String loginId) {

        // 필요 없을시 ux에서 안보이게 제한 하겠습니다.
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UnauthorizedException("로그인이 필요한 서비스입니다."));

        Festival festival = festivalRepository.findById(festivalId)
                .orElseThrow(() -> new CustomNotFoundException("해당 축제를 찾을 수 없습니다."));

        boolean alreadyBookmarked =
                festivalBookmarkRepository.existsByMemberIdAndFestivalId(member.getId(), festivalId);

        if (alreadyBookmarked) {
            throw new ConflictException("이미 찜한 축제입니다.");
        }

        FestivalBookmark festivalBookmark = new FestivalBookmark(member, festival);
        festivalBookmarkRepository.save(festivalBookmark);

        festivalRepository.increaseBookmarkCount(festivalId);

        return new FestivalBookmarkResponseDto(
                festival.getId(),
                member.getId(),
                true,
                festival.getBookMarkCount()+1
        );
    }

    @Transactional
    public FestivalBookmarkResponseDto cancelBookmark(Long festivalId, String loginId) {

        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UnauthorizedException("로그인이 필요한 서비스입니다."));

        Festival festival = festivalRepository.findById(festivalId)
                .orElseThrow(() -> new CustomNotFoundException("해당 축제를 찾을 수 없습니다."));

        FestivalBookmark bookmark = festivalBookmarkRepository
                .findByMemberIdAndFestivalId(member.getId(), festivalId)
                .orElseThrow(() -> new BadRequestException("찜하지 않은 축제입니다."));

        festivalBookmarkRepository.delete(bookmark);
        festivalBookmarkRepository.flush();

        festivalRepository.decreaseBookmarkCount(festivalId);

        return new FestivalBookmarkResponseDto(
                festival.getId(),
                member.getId(),
                false,
                Math.max(0, festival.getBookMarkCount() - 1)
        );
    }
}
