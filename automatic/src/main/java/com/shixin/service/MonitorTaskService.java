package com.shixin.service;

import java.util.List;

import com.shixin.entity.MonitorTask;
import com.shixin.entity.MonitorTaskListDTO;

public interface MonitorTaskService {
     // 获取所有监控任务
    List<MonitorTask> getAllTasks();

    // 更新任务的关键词
    int updateTaskKeywords(Long id, String keywords);

    // 根据ID查询任务
    MonitorTask getTaskById(Long id);
    // 保存或更新任务
    MonitorTask saveOrUpdateTask(MonitorTask task);

    MonitorTask saveTask(MonitorTask monitorTask);

    List<MonitorTaskListDTO> getTasksByUserId(Long userId);

    Boolean deleteTaskById(Long taskId, Long userId);
}
