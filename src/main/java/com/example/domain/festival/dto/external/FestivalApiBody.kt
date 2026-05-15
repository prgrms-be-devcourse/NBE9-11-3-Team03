package com.example.domain.festival.dto.external

import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

class FestivalApiBody {
    private var items: FestivalApiItems? = null

    var numOfRows: Int = 0
    var pageNo: Int = 0
    var totalCount: Int = 0

    fun getItems(): FestivalApiItems? = items

    @JsonSetter("items")
    fun setItems(itemsNode: JsonNode?) {
        if (itemsNode == null || itemsNode.isNull) {
            items = null
            return
        }

        if (itemsNode.isTextual && itemsNode.asText().isBlank()) {
            items = null
            return
        }

        items = try {
            OBJECT_MAPPER.treeToValue(itemsNode, FestivalApiItems::class.java)
        } catch (e: Exception) {
            println("items 파싱 실패: $itemsNode")
            e.printStackTrace()
            null
        }
    }

    companion object {
        private val OBJECT_MAPPER: ObjectMapper = ObjectMapper()
            .registerKotlinModule()
    }
}