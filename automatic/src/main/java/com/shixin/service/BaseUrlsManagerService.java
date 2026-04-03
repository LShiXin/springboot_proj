package com.shixin.service;

import java.util.List;

import com.shixin.entity.ConfigItem;

public interface BaseUrlsManagerService {
    List<ConfigItem> loadPotional_Urls();
    
    /**
     * 强制从JSON文件重新加载数据到Redis
     * @return 加载的配置项列表
     */
    List<ConfigItem> reloadPotional_UrlsFromJson();
}
