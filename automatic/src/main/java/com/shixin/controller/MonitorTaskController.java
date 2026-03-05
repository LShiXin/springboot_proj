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

    /*
    更新任务，传入的 MonitorTask 对象必须包含 ID、name、keywords 和 scheduleConfig 字段，
    且 ID 必须对应一个已存在的任务。更新操作会检查用户权限，确保用户只能更新自己的任务。
    更新成功后返回更新后的任务信息，如果请求数据错误或更新失败则返回相应的错误信息。
    */
    @PostMapping("/monitottask/update")
    public ResponseEntity<ApiResponse<MonitorTaskListDTO>> updateMonitorTask(@RequestBody MonitorTask monitorTask,
            HttpServletRequest request) {
        User user = (User) request.getAttribute("userInfo");
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未授权"));
        }
        if (monitorTask == null || monitorTask.getId() == null || monitorTask.getName() == null
                || monitorTask.getKeywords() == null || monitorTask.getScheduleConfig() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "请求数据错误"));
        }
        try {
            MonitorTask updatedTask = monitorTaskService.updateTaskByUser(monitorTask, user.getId());
            MonitorTaskListDTO dto = new MonitorTaskListDTO(updatedTask.getId(), updatedTask.getName(),
                    updatedTask.getKeywords(), updatedTask.isEnabled(), updatedTask.getScheduleConfig().getStartTime(),
                    updatedTask.getScheduleConfig().getTimePoint(),
                    updatedTask.getScheduleConfig().getIntervalMillis() / 60000,
                    updatedTask.getScheduleConfig().getEndTime());
            return ResponseEntity.ok(ApiResponse.success(dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }


    @PostMapping("/monitottask/handleActivate")
    public ResponseEntity<ApiResponse<String>> handleActivate(@RequestBody Long task_id,HttpServletRequest request) {
        //TODO: process POST request
        System.out.println("接收到的任务ID：" + task_id);
        User user = (User) request.getAttribute("userInfo");
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未授权"));
        }
        //获取编辑的任务
        MonitorTask entity = monitorTaskService.getTaskById(task_id);
        if (entity == null || !entity.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "任务未找到或无权限"));
        }else {
            //切换任务状态
            entity.setEnabled(!entity.isEnabled());
            monitorTaskService.updateTaskByUser(entity, user.getId());
            return ResponseEntity.ok(ApiResponse.success("任务状态已切换"));
        }
    }
    

}



