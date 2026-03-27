package com.zjgsu.whattoeat.integration.amap;

import java.util.List;

public interface AmapClient {

    /**
     * 搜索附近餐厅 POI
     *
     * @param longitude 经度 (GCJ-02)
     * @param latitude  纬度 (GCJ-02)
     * @param radius    搜索半径（米）
     * @param page      页码（从 1 开始）
     * @param pageSize  每页数量
     */
    AmapSearchResult searchNearby(double longitude, double latitude, int radius, int page, int pageSize);

    /**
     * 按关键词搜索餐厅 POI
     */
    AmapSearchResult searchByKeyword(String keyword, double longitude, double latitude, int radius, int page, int pageSize);

    record AmapSearchResult(List<AmapPoi> items, long total) {
    }
}
