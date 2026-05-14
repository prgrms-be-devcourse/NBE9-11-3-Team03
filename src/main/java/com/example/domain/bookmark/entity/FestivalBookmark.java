package com.example.domain.bookmark.entity;

import com.example.domain.festival.entity.Festival;
import com.example.domain.member.entity.Member;
import com.example.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_festival_bookmark_member_festival",
                        columnNames = {"member_id", "festival_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FestivalBookmark extends BaseCreatedEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "festival_id", nullable = false)
    private Festival festival;

    //TODOS 생성자 구현
    public FestivalBookmark(Member member, Festival festival) {
        this.member = member;
        this.festival = festival;
    }
}