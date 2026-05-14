package com.example.domain.festival.dto.external;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FestivalApiBody {
    private FestivalApiItems items;
    private int numOfRows;
    private int pageNo;
    private int totalCount;

    @JsonSetter("items")
    public void setItems(JsonNode itemsNode) {
        if (itemsNode == null || itemsNode.isNull()) {
            this.items = null;
            return;
        }

        // 빈 페이지에서 "items": "" 로 오는 경우
        if (itemsNode.isTextual() && itemsNode.asText().isBlank()) {
            this.items = null;
            return;
        }

        // 정상 페이지에서 객체로 오는 경우
        try {
            this.items = new ObjectMapper().treeToValue(itemsNode, FestivalApiItems.class);
        } catch (Exception e) {
            System.out.println("items 파싱 실패: " + itemsNode);
            e.printStackTrace();
            this.items = null;
        }
    }

}
