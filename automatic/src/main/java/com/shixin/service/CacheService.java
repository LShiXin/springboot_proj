package com.shixin.service;

import java.util.List;

import com.shixin.entity.MonitorTaskListDTO;
import com.shixin.entity.MonitorUrlListDTO;

public interface CacheService {

    // 获取当前用户下的所有监控任务,将数据放到redis中,并设置过期时间为10分钟
    void cacheMonitorTasksToRedis(Long userId);

    // 获取当前用户下的所有监控链接,将数据放到redis中,并设置过期时间为10分钟
    void cacheMonitorTaskUrlsToRedis(Long userId);

    // 根据用户ID查询所有监控任务，并转换为 MonitorTaskListDTO
    List<MonitorTaskListDTO> getTasksByUserId(Long userId);

    // 根据用户ID查询所有监控链接，并转换为 MonitorUrlListDTO
    List<MonitorUrlListDTO> findListDtoByUserId(Long userId);
}
