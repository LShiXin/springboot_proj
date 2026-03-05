package com.shixin.serviceimpl;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.shixin.entity.MonitorTask;
import com.shixin.entity.MonitorTaskListDTO;
import com.shixin.repository.MonitorTaskRepository;
import com.shixin.service.MonitorTaskService;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class MonitorTaskServiceimpl implements MonitorTaskService {

    @Autowired
    private MonitorTaskRepository monitorTaskRepository;
    
    // 获取所有监控任务
    @Override 
    public List<MonitorTask> getAllTasks() {
        return monitorTaskRepository.findAll();
    }

    // 更新任务的关键词
    @Override 
    public int updateTaskKeywords(Long id, String keywords) {
        return monitorTaskRepository.updateKeywordsById(keywords, id);
    }   

    // 根据ID查询任务
    @Override 
    public MonitorTask getTaskById(Long id) {
        return monitorTaskRepository.findById(id).orElse(null);
    }

    // 保存或更新任务
    @Override 
    public MonitorTask saveOrUpdateTask(MonitorTask task) {
        return monitorTaskRepository.save(task);
    }

    @Override
    public MonitorTask saveTask(MonitorTask monitorTask) {
        return monitorTaskRepository.save(monitorTask);
    }

    @Override
    public List<MonitorTaskListDTO> getTasksByUserId(Long userId) {
        return monitorTaskRepository.findAllByUserId(userId);
    }

    @Override
    public Boolean deleteTaskById(Long taskId, Long userId) {
        try {
            MonitorTask entity = monitorTaskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("该用户下未发现定时任务"));
            if (entity != null && entity.getUser().getId().equals(userId)) {
                monitorTaskRepository.delete(entity);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public MonitorTask updateTaskByUser(MonitorTask monitorTask, Long userId) {
        if (monitorTask == null || monitorTask.getId() == null) {
            throw new RuntimeException("任务参数不正确");
        }
        MonitorTask entity = monitorTaskRepository.findById(monitorTask.getId())
                .orElseThrow(() -> new RuntimeException("任务不存在"));
        if (entity.getUser() == null || !entity.getUser().getId().equals(userId)) {
            throw new RuntimeException("无权限编辑该任务");
        }

        entity.setName(monitorTask.getName());
        entity.setKeywords(monitorTask.getKeywords());

        if (entity.getScheduleConfig() != null && monitorTask.getScheduleConfig() != null) {
            entity.getScheduleConfig().setStartTime(monitorTask.getScheduleConfig().getStartTime());
            entity.getScheduleConfig().setEndTime(monitorTask.getScheduleConfig().getEndTime());
            entity.getScheduleConfig().setIntervalMillis(monitorTask.getScheduleConfig().getIntervalMillis());
        }
        // 按需求：状态字段不在本次编辑范围内，不更新 enabled
        return monitorTaskRepository.save(entity);
    }
}   
