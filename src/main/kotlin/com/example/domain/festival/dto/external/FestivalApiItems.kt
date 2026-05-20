package com.example.domain.festival.dto.external

import com.fasterxml.jackson.annotation.JsonFormat

data class FestivalApiItems(
    @JsonFormat(with = [JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY])
    var item: List<FestivalApiItem>? = null
)