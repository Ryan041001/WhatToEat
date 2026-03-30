package com.zjgsu.whattoeat.service.domain;

import com.zjgsu.whattoeat.integration.amap.AmapPoi;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RecommendationDomainService {

    /**
     * 从候选列表中过滤黑名单，返回可推荐的 POI
     */
    public List<AmapPoi> filterBlacklist(List<AmapPoi> candidates, Set<String> blacklistedPoiIds) {
        if (blacklistedPoiIds == null || blacklistedPoiIds.isEmpty()) {
            return candidates;
        }
        return candidates.stream()
                .filter(candidate -> !blacklistedPoiIds.contains(candidate.poiId()))
                .toList();
    }

    /**
     * 从过滤后的列表中随机选取一个
     */
    public AmapPoi pickRandom(List<AmapPoi> candidates) {
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }
}
