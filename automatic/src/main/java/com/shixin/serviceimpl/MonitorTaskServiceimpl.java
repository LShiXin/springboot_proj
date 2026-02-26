package com.shixin.serviceimpl;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.shixin.entity.MonitorTask;
import com.shixin.repository.MonitorTaskRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class MonitorTaskServiceimpl {

    @Autowired
    private MonitorTaskRepository monitorTaskRepository;
    
    // 获取所有监控任务
    public List<MonitorTask> getAllTasks() {
        return monitorTaskRepository.findAll();
    }

    // 更新任务的关键词
    public int updateTaskKeywords(Long id, String keywords) {
        return monitorTaskRepository.updateKeywordsById(keywords, id);
    }   

    // 根据ID查询任务
    public MonitorTask getTaskById(Long id) {
        return monitorTaskRepository.findById(id).orElse(null);
    }

    // 保存或更新任务
    public MonitorTask saveOrUpdateTask(MonitorTask task) {
        return monitorTaskRepository.save(task);
    }
}   
