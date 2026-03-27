package com.zjgsu.whattoeat.integration.amap;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.config.AmapProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Component
public class AmapHttpClient implements AmapClient {

    private static final String AROUND_SEARCH_PATH = "/v3/place/around";

    private final RestClient restClient;
    private final AmapProperties props;

    public AmapHttpClient(RestClient.Builder builder, AmapProperties props) {
        this.props = props;
        this.restClient = builder
                .baseUrl(props.baseUrl())
                .build();
    }

    @Override
    public AmapSearchResult searchNearby(double longitude, double latitude, int radius, int page, int pageSize) {
        return search(longitude, latitude, radius, page, pageSize, null);
    }

    @Override
    public AmapSearchResult searchByKeyword(String keyword, double longitude, double latitude, int radius, int page, int pageSize) {
        return search(longitude, latitude, radius, page, pageSize, keyword);
    }

    private AmapSearchResult search(double longitude, double latitude, int radius, int page, int pageSize, String keyword) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path(AROUND_SEARCH_PATH)
                                .queryParam("key", props.key())
                                .queryParam("location", longitude + "," + latitude)
                                .queryParam("radius", radius)
                                .queryParam("types", "050000")
                                .queryParam("offset", pageSize)
                                .queryParam("page", page)
                                .queryParam("output", "json");
                        if (keyword != null && !keyword.isBlank()) {
                            builder.queryParam("keywords", keyword);
                        }
                        return builder.build();
                    })
                    .retrieve()
                    .body(Map.class);

            if (body == null || !"1".equals(String.valueOf(body.get("status")))) {
                throw new BusinessException(ErrorCode.AMAP_UPSTREAM_ERROR);
            }

            long total = parseCount(body.get("count"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> pois = (List<Map<String, Object>>) body.get("pois");
            if (pois == null || pois.isEmpty()) {
                return new AmapSearchResult(Collections.emptyList(), total);
            }

            return new AmapSearchResult(pois.stream().map(this::toPoi).toList(), total);
        } catch (BusinessException e) {
            throw e;
        } catch (ResourceAccessException e) {
            if (isTimeoutException(e)) {
                throw new BusinessException(ErrorCode.AMAP_UPSTREAM_TIMEOUT, e.getMessage());
            }
            throw new BusinessException(ErrorCode.AMAP_UPSTREAM_ERROR, e.getMessage());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AMAP_UPSTREAM_ERROR, e.getMessage());
        }
    }

    private boolean isTimeoutException(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof SocketTimeoutException
                    || current instanceof HttpTimeoutException
                    || current instanceof TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private long parseCount(Object countRaw) {
        if (countRaw == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(countRaw));
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private AmapPoi toPoi(Map<String, Object> raw) {
        String location = String.valueOf(raw.getOrDefault("location", "0,0"));
        String[] parts = location.split(",");
        double lng = parts.length > 0 ? Double.parseDouble(parts[0]) : 0;
        double lat = parts.length > 1 ? Double.parseDouble(parts[1]) : 0;
        return new AmapPoi(
                String.valueOf(raw.getOrDefault("id", "")),
                String.valueOf(raw.getOrDefault("name", "")),
                String.valueOf(raw.getOrDefault("address", "")),
                lng,
                lat,
                String.valueOf(raw.getOrDefault("type", "")),
                Double.parseDouble(String.valueOf(raw.getOrDefault("distance", "0")))
        );
    }
}
