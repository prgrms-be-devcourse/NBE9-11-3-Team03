package com.example.domain.festival.repository;

import com.example.domain.festival.dto.FestivalSearchRequestDto;
import com.example.domain.festival.entity.Festival;
import com.example.domain.festival.entity.FestivalStatus;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static com.example.domain.festival.entity.QFestival.festival;

@AllArgsConstructor
public class FestivalRepositoryImpl implements FestivalRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Festival> searchFestivals(FestivalSearchRequestDto searchDto, Pageable pageable) {

        List<Festival> content = queryFactory
                .selectFrom(festival)
                .where(
                        regionCodeEquals(searchDto.regionCode()),
                        statusEquals(searchDto.status()),
                        monthEquals(searchDto.month()),
                        keywordContains(searchDto.keyword()),
                        nearBy(searchDto.mapX(), searchDto.mapY(), searchDto.radiusKm())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(festivalSort(pageable))
                .fetch();

        JPAQuery<Long> contentQuery = queryFactory
                .select(festival.count())
                .from(festival)
                .where(
                        regionCodeEquals(searchDto.regionCode()),
                        statusEquals(searchDto.status()),
                        monthEquals(searchDto.month()),
                        keywordContains(searchDto.keyword()),
                        nearBy(searchDto.mapX(), searchDto.mapY(), searchDto.radiusKm())
                );

        return PageableExecutionUtils.getPage(content, pageable, contentQuery::fetchOne);
    }

    @Override
    public List<Festival> findNearbyFestivals(FestivalSearchRequestDto searchDto) {
        return queryFactory
                .selectFrom(festival)
                .where(
                        festival.status.in(FestivalStatus.ONGOING, FestivalStatus.UPCOMING),
                        nearBy(searchDto.mapX(), searchDto.mapY(), searchDto.radiusKm())
                )
                .fetch();
    }

    private OrderSpecifier<?>[] festivalSort(Pageable pageable) {
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        //1순위 룰 -> sort 조건과 관계없이 상태값 정렬(진행중>예정>종료)
        NumberExpression<Integer> statusPriority = new CaseBuilder()
                .when(festival.status.eq(FestivalStatus.ONGOING)).then(1)
                .when(festival.status.eq(FestivalStatus.UPCOMING)).then(2)
                .otherwise(3);

        orderSpecifiers.add(statusPriority.asc());

        //2순위
        if(!pageable.getSort().isEmpty()) {
            //조회순/북마크순 정렬
            for (Sort.Order order : pageable.getSort()) {
                Order direction = order.getDirection().isAscending() ? Order.ASC : Order.DESC;
                switch (order.getProperty()) {
                    case "viewCount" -> orderSpecifiers.add(new OrderSpecifier<>(direction, festival.viewCount));
                    case "startDate" -> orderSpecifiers.add(new OrderSpecifier<>(direction, festival.startDate));
                    case "bookMarkCount" ->
                            orderSpecifiers.add(new OrderSpecifier<>(direction, festival.bookMarkCount));
                }
            }
        }else{
            //기본정렬 -> 아무조건도 없거나, 필터링(키워드/지역/월) 만 있을떄

            //Onging, Upcoming 인 경우 startDate 오름차순, 나머지는 null로 처리하여 마지막에 오도록
            orderSpecifiers.add(
                    new CaseBuilder()
                            .when(festival.status.in(FestivalStatus.ONGOING, FestivalStatus.UPCOMING))
                            .then(festival.startDate)
                            .otherwise(Expressions.nullExpression(LocalDateTime.class))
                            .asc().nullsLast()
            );
            //Eneded 인 경우 endDate 오름차순, 나머지는 null로 처리하여 마지막에 오도록
            orderSpecifiers.add(
                    new CaseBuilder()
                            .when(festival.status.eq(FestivalStatus.ENDED))
                            .then(festival.endDate)
                            .otherwise(Expressions.nullExpression(LocalDateTime.class))
                            .desc().nullsLast()
            );
        }
        return orderSpecifiers.toArray(new OrderSpecifier[0]);
    }

    public BooleanExpression nearBy(Double myMapx, Double myMapY, Double radiusKm){
        if(myMapx==null||myMapY==null||radiusKm==null){
            return null;
        }
        double latChange = radiusKm / 111.0; // 위도(Y) 변화량
        double lonChange = radiusKm / 88.0;  // 경도(X) 변화량

        double minX = myMapx - lonChange;
        double maxX = myMapx + lonChange;
        double minY = myMapY - latChange;
        double maxY = myMapY + latChange;

        return festival.mapX.between(minX, maxX)
                .and(festival.mapY.between(minY, maxY));
    }

    private BooleanExpression keywordContains(String keyword) {
        return StringUtils.hasText(keyword)?festival.title.contains(keyword):null;
    }

    private BooleanExpression monthEquals(Integer month) {
        if(month==null||month<1||month>12) return null;
        int currentYear = LocalDateTime.now().getYear();
        YearMonth targetMonth = YearMonth.of(currentYear, month);

        LocalDateTime startOfMonth = targetMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = targetMonth.atEndOfMonth().atTime(23, 59, 59, 999999999);

        return festival.startDate.loe(endOfMonth).and(festival.endDate.goe(startOfMonth));
    }

    private BooleanExpression statusEquals(FestivalStatus status) {
        return status !=null?festival.status.eq(status):null;

    }

    private BooleanExpression regionCodeEquals(String regionCode) {
        if(!StringUtils.hasText(regionCode))return null;

        return StringUtils.hasText(regionCode)?festival.lDongRegnCd.eq(regionCode):null;
    }
}
