package com.zjgsu.whattoeat.service.application;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.integration.amap.AmapClient;
import com.zjgsu.whattoeat.integration.amap.AmapPoi;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RestaurantQueryApplicationService {

    private final AmapClient amapClient;
    private final MeterRegistry meterRegistry;

    public RestaurantQueryApplicationService(AmapClient amapClient, MeterRegistry meterRegistry) {
        this.amapClient = amapClient;
        this.meterRegistry = meterRegistry;
    }

    public RestaurantPage nearby(double longitude, double latitude, int radius, int page, int size) {
        try {
            AmapClient.AmapSearchResult result = amapClient.searchNearby(longitude, latitude, radius, page, size);
            if (result.total() == 0) {
                throw new BusinessException(ErrorCode.AMAP_NO_RESULT);
            }
            incrementRequestCounter("nearby", "success");
            return new RestaurantPage(result.items(), page, size, result.total());
        } catch (BusinessException e) {
            incrementRequestCounter("nearby", e.getErrorCode().name());
            throw e;
        } catch (RuntimeException e) {
            incrementRequestCounter("nearby", ErrorCode.SYSTEM_ERROR.name());
            throw e;
        }
    }

    public RestaurantPage search(String keyword, double longitude, double latitude, int radius, int page, int size) {
        try {
            AmapClient.AmapSearchResult result = amapClient.searchByKeyword(keyword, longitude, latitude, radius, page, size);
            if (result.total() == 0) {
                throw new BusinessException(ErrorCode.AMAP_NO_RESULT);
            }
            incrementRequestCounter("search", "success");
            return new RestaurantPage(result.items(), page, size, result.total());
        } catch (BusinessException e) {
            incrementRequestCounter("search", e.getErrorCode().name());
            throw e;
        } catch (RuntimeException e) {
            incrementRequestCounter("search", ErrorCode.SYSTEM_ERROR.name());
            throw e;
        }
    }

    private void incrementRequestCounter(String scene, String result) {
        Counter.builder("restaurant.query.requests")
                .tag("scene", scene)
                .tag("result", result)
                .register(meterRegistry)
                .increment();
    }

    public record RestaurantPage(List<AmapPoi> items, int page, int size, long total) {
    }
}
