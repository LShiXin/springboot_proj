package com.shixin.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shixin.entity.MonitorUrl;
public interface MonitorUrlRepository extends JpaRepository<MonitorUrl, Long> {
    // 根据任务ID查询链接
    List<MonitorUrl> findByTaskId(Long taskId);
    
    // 查询所有启用的链接
    List<MonitorUrl> findByEnabledTrue();
    
    // 根据任务和启用状态查询
    List<MonitorUrl> findByTaskIdAndEnabledTrue(Long taskId);
    
}
