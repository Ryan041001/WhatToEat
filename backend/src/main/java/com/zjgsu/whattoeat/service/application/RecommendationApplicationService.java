package com.zjgsu.whattoeat.service.application;

import com.zjgsu.whattoeat.integration.amap.AmapPoi;
import org.springframework.stereotype.Service;

@Service
public class RecommendationApplicationService {

    /**
     * 根据用户位置和偏好，返回一个随机推荐餐厅
     */
    public AmapPoi recommend(Long userId, double longitude, double latitude, int radius) {
        throw new UnsupportedOperationException("TODO");
    }
}
