package com.zjgsu.whattoeat.integration.amap;

public record AmapPoi(
        String poiId,
        String name,
        String address,
        double longitude,
        double latitude,
        String category,
        double distance
) {
}
