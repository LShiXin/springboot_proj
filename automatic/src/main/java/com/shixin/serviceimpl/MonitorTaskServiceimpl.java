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
}   
