package com.zjgsu.whattoeat.service.application;

import com.zjgsu.whattoeat.common.error.BusinessException;
import com.zjgsu.whattoeat.common.error.ErrorCode;
import com.zjgsu.whattoeat.integration.amap.AmapClient;
import com.zjgsu.whattoeat.integration.amap.AmapPoi;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RestaurantQueryApplicationService {

    private final AmapClient amapClient;

    public RestaurantQueryApplicationService(AmapClient amapClient) {
        this.amapClient = amapClient;
    }

    public RestaurantPage nearby(double longitude, double latitude, int radius, int page, int size) {
        AmapClient.AmapSearchResult result = amapClient.searchNearby(longitude, latitude, radius, page, size);
        if (result.total() == 0) {
            throw new BusinessException(ErrorCode.AMAP_NO_RESULT);
        }
        return new RestaurantPage(result.items(), page, size, result.total());
    }

    public RestaurantPage search(String keyword, double longitude, double latitude, int radius, int page, int size) {
        AmapClient.AmapSearchResult result = amapClient.searchByKeyword(keyword, longitude, latitude, radius, page, size);
        if (result.total() == 0) {
            throw new BusinessException(ErrorCode.AMAP_NO_RESULT);
        }
        return new RestaurantPage(result.items(), page, size, result.total());
    }

    public record RestaurantPage(List<AmapPoi> items, int page, int size, long total) {
    }
}
