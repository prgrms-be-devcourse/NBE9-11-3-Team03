package com.example.domain.festival.dto.external

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class FestivalApiItem(
    var contentid: String? = null,
    var title: String? = null,
    var tel: String? = null,
    var addr1: String? = null,
    var addr2: String? = null,
    var homepage: String? = null,
    var overview: String? = null,
    var firstimage: String? = null,
    var firstimage2: String? = null,
    var mapx: String? = null,
    var mapy: String? = null,

    @JsonProperty("lDongRegnCd")
    var lDongRegnCd: String? = null,

    var eventstartdate: String? = null,
    var eventenddate: String? = null,
    var modifiedtime: String? = null
)