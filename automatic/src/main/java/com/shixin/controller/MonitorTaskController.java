package com.shixin.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.shixin.entity.ApiResponse;
import com.shixin.entity.MonitorTask;
import com.shixin.entity.MonitorTaskListDTO;
import com.shixin.entity.ScheduleType;
import com.shixin.entity.User;
import com.shixin.service.MonitorTaskService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class MonitorTaskController {

    @Autowired
    private MonitorTaskService monitorTaskService;
    
    // 获取当前用户的所有监控任务
    @PostMapping("/monitottask/getbyuser")
    public ResponseEntity<ApiResponse<List<MonitorTaskListDTO>>> getTasksByUser(HttpServletRequest request) {
        User user = (User) request.getAttribute("userInfo");
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未授权"));
        }
        List<MonitorTaskListDTO> tasks = monitorTaskService.getTasksByUserId(user.getId());
        System.out.println("获取到的任务列表：" + tasks);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }
    

    // 新增任务（含定时任务配置）
    @PostMapping("/monitottask/add")
    public ResponseEntity<ApiResponse<MonitorTaskListDTO>> addMonitorTask(@RequestBody MonitorTask monitorTask,HttpServletRequest request) {
        // 这里假设 monitorTaskService.saveTask 会保存任务及其关联的定时任务配置
        if (monitorTask == null || monitorTask.getName() == null || monitorTask.getKeywords() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "请求数据错误"));
        }else {
            monitorTask.setUser((User) request.getAttribute("userInfo"));

            // 判断定时任务的的类型

            System.out.println("保存的任务" + monitorTask);
            monitorTask.getScheduleConfig().setScheduleType(ScheduleType.FIXED_RATE); // 这里假设默认使用 CRON 类型，实际可以根据前端传来的数据进行设置
            System.out.println("保存的任务配置" + monitorTask.getScheduleConfig());
            MonitorTask savedTask =monitorTaskService.saveTask(monitorTask);
            MonitorTaskListDTO dto = new MonitorTaskListDTO(savedTask.getId(), savedTask.getName(), savedTask.getKeywords(),
                    savedTask.isEnabled(), savedTask.getScheduleConfig().getStartTime(),
                    savedTask.getScheduleConfig().getTimePoint(),
                    savedTask.getScheduleConfig().getIntervalMillis()/60000, // 转换为分钟
                    savedTask.getScheduleConfig().getEndTime());
            return ResponseEntity.ok(ApiResponse.success(dto));
        }
    }


    // 根据任务ID删除任务,和任务下面对应的链接
    @PostMapping("/monitottask/handleDelete")
    public ResponseEntity<ApiResponse<Boolean>> deleteMonitorTask(@RequestBody Long taskId,HttpServletRequest request) {
        User user = (User) request.getAttribute("userInfo");
        System.out.println("删除任务的用户信息：");
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未授权"));
        }
        boolean deleted = monitorTaskService.deleteTaskById(taskId, user.getId());
        if (deleted) {
            return ResponseEntity.ok(ApiResponse.success(true));
        } else {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "任务未找到或删除失败"));
        }
    }


}


