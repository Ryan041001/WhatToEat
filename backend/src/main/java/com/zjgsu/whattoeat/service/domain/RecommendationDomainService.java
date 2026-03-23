package com.zjgsu.whattoeat.service.domain;

import com.zjgsu.whattoeat.integration.amap.AmapPoi;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class RecommendationDomainService {

    /**
     * 从候选列表中过滤黑名单，返回可推荐的 POI
     */
    public List<AmapPoi> filterBlacklist(List<AmapPoi> candidates, Set<String> blacklistedPoiIds) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * 从过滤后的列表中随机选取一个
     */
    public AmapPoi pickRandom(List<AmapPoi> candidates) {
        throw new UnsupportedOperationException("TODO");
    }
}
