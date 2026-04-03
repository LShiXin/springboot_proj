package com.shixin.service;

import java.util.List;

import com.shixin.entity.ConfigItem;
import com.shixin.entity.MonitorTask;
import com.shixin.entity.MonitorTaskListDTO;
import com.shixin.entity.MonitorUrl;
import com.shixin.entity.MonitorUrlListDTO;

public interface MonitorTaskService {
     // 获取所有监控任务
    List<MonitorTask> getAllTasks();

    // 根据ID查询任务
    MonitorTask getTaskById(Long id);
    // 保存或更新任务
    MonitorTask saveOrUpdateTask(MonitorTask task);

    List<MonitorTaskListDTO> getTasksByUserId(Long userId);
    List<MonitorUrlListDTO> findListDtoByUserId(Long userId);

    Boolean deleteTaskById(Long taskId, Long userId);

    MonitorTask updateTaskByUser(MonitorTask monitorTask, Long userId);
    
    // 获取对应的监控任务的全部监控链接
    List<MonitorUrl> findAllTaskUrlsByTaskId(Long TaskID); 

    MonitorUrl save_MonitorUrl(MonitorUrl url);
    
    // 删除监控链接
    Boolean deleteLinkById(Long linkId, Long userId);
    
    // 切换链接状态
    MonitorUrl toggleLinkStatus(Long linkId, Long userId);
}
