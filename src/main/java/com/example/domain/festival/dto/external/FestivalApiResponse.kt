package com.example.domain.festival.dto.external;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FestivalApiResponse {
    private Response response;

    @Getter
    @NoArgsConstructor
    public static class Response {
        private FestivalApiHeader header;
        private FestivalApiBody body;
    }
}
