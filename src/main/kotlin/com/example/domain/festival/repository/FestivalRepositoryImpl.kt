package com.example.domain.festival.repository

import com.example.domain.festival.dto.request.FestivalSearchRequest
import com.example.domain.festival.entity.Festival
import com.example.domain.festival.entity.FestivalStatus
import com.example.domain.festival.entity.QFestival
import com.querydsl.core.types.Order
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.CaseBuilder
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.support.PageableExecutionUtils
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.YearMonth

@Repository
class FestivalRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : FestivalRepositoryCustom {

    override fun searchFestivals(searchDto: FestivalSearchRequest, pageable: Pageable): Page<Festival> {
        val festival = QFestival.festival
        val predicates = arrayOf(
            regionCodeEquals(searchDto.regionCode),
            statusEquals(searchDto.status),
            monthEquals(searchDto.month),
            keywordContains(searchDto.keyword),
            nearBy(searchDto.mapX, searchDto.mapY, searchDto.radiusKm),
        )

        val content = queryFactory
            .selectFrom(festival)
            .where(*predicates)
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .orderBy(*festivalSort(pageable))
            .fetch()

        val contentQuery = queryFactory
            .select(festival.count())
            .from(festival)
            .where(*predicates)


        return PageableExecutionUtils.getPage(content, pageable) { contentQuery.fetchOne() ?: 0L }
    }

    override fun findNearbyFestivals(searchDto: FestivalSearchRequest): List<Festival> {
        return queryFactory
            .selectFrom(QFestival.festival)
            .where(
                QFestival.festival.status.`in`(FestivalStatus.ONGOING, FestivalStatus.UPCOMING),
                nearBy(searchDto.mapX, searchDto.mapY, searchDto.radiusKm)
            )
            .fetch()
    }

    private fun festivalSort(pageable: Pageable): Array<OrderSpecifier<*>> {
        val festival = QFestival.festival
        val specs = mutableListOf<OrderSpecifier<*>>()

        //1순위 룰 -> sort 조건과 관계없이 상태값 정렬(진행중>예정>종료)
        specs += CaseBuilder()
            .`when`(festival.status.eq(FestivalStatus.ONGOING)).then(1)
            .`when`(festival.status.eq(FestivalStatus.UPCOMING)).then(2)
            .otherwise(3)
            .asc()

        //2순위
        if (!pageable.sort.isEmpty) {
            pageable.sort.forEach { order ->
                val direction = if (order.direction.isAscending) Order.ASC else Order.DESC
                when (order.property) {
                    "viewCount" -> specs += OrderSpecifier(direction, festival.viewCount)
                    "startDate" -> specs += OrderSpecifier(direction, festival.startDate)
                    "bookMarkCount" -> specs += OrderSpecifier(direction, festival.bookMarkCount)
                }
            }
        } else {
            //기본정렬 -> 아무조건도 없거나, 필터링(키워드/지역/월) 만 있을떄
            //Onging, Upcoming 인 경우 startDate 오름차순, 나머지는 null로 처리하여 마지막에 오도록
            specs += CaseBuilder()
                .`when`(festival.status.`in`(FestivalStatus.ONGOING, FestivalStatus.UPCOMING))
                .then(festival.startDate)
                .otherwise(Expressions.nullExpression(LocalDateTime::class.java))
                .asc().nullsLast()

            specs += CaseBuilder()
                .`when`(festival.status.eq(FestivalStatus.ENDED))
                .then(festival.endDate)
                .otherwise(Expressions.nullExpression(LocalDateTime::class.java))
                .desc().nullsLast()
        }
        return specs.toTypedArray()
    }

    private fun nearBy(myMapX: Double?, myMapY: Double?, radiusKm: Double?): BooleanExpression? {
        if (myMapX == null || myMapY == null || radiusKm == null) return null
        val festival = QFestival.festival
        val latChange = radiusKm / 111.0 // 위도(Y) 변화량
        val lonChange = radiusKm / 88.0 // 경도(X) 변화량

        return festival.mapX.between(myMapX - lonChange, myMapX + lonChange)
            .and(festival.mapY.between(myMapY - latChange, myMapY + latChange))
    }

    private fun keywordContains(keyword: String?): BooleanExpression? =
        keyword?.takeIf { it.isNotBlank() }
            ?.let { QFestival.festival.title.contains(it) }


    private fun monthEquals(month: Int?): BooleanExpression? {
        if (month == null || month !in 1..12) return null
        val targetMonth = YearMonth.of(LocalDateTime.now().year, month)

        val startOfMonth = targetMonth.atDay(1).atStartOfDay()
        val endOfMonth = targetMonth.atEndOfMonth().atTime(23, 59, 59, 999_999_999)

        return QFestival.festival.startDate.loe(endOfMonth).and(QFestival.festival.endDate.goe(startOfMonth))
    }

    private fun statusEquals(status: FestivalStatus?): BooleanExpression? =
        status?.let { QFestival.festival.status.eq(it) }


    private fun regionCodeEquals(regionCode: String?): BooleanExpression? =
        regionCode?.takeIf { it.isNotBlank() }
            ?.let { QFestival.festival.lDongRegnCd.eq(it) }
}
