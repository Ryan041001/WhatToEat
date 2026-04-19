package com.zjgsu.whattoeat.domain.recommendation;

import com.zjgsu.whattoeat.integration.amap.AmapPoi;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RecommendationDomainService {

    /**
     * 从过滤后的列表中随机选取一个
     */
    public AmapPoi pickRandom(List<AmapPoi> candidates) {
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }
}
