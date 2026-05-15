package com.example.domain.festival.dto.external

data class FestivalApiResponse(
    var response: Response? = null
) {
    data class Response(
        var header: FestivalApiHeader? = null,
        var body: FestivalApiBody? = null
    )
}