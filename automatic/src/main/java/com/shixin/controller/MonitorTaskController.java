package com.shixin.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shixin.entity.ApiResponse;
import com.shixin.entity.ConfigItem;
import com.shixin.entity.MonitorTask;
import com.shixin.entity.MonitorTaskListDTO;
import com.shixin.entity.MonitorUrl;
import com.shixin.entity.MonitorUrlListDTO;
import com.shixin.entity.ScheduleType;
import com.shixin.entity.User;
import com.shixin.service.MonitorTaskService;
import com.shixin.service.BaseUrlsManagerService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
public class MonitorTaskController {

    @Autowired
    private MonitorTaskService monitorTaskService;

    @Autowired
    private BaseUrlsManagerService baseUrlsManagerService;
    
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
            MonitorTask savedTask =monitorTaskService.saveOrUpdateTask(monitorTask);
            MonitorTaskListDTO dto = new MonitorTaskListDTO(savedTask.getId(), savedTask.getName(), savedTask.getKeywords(),
                    savedTask.isEnabled(), savedTask.getScheduleConfig().getStartTime(),
                    savedTask.getScheduleConfig().getTimePoint(),
                    savedTask.getScheduleConfig().getIntervalMillis()/60000, // 转换为分钟
                    savedTask.getScheduleConfig().getEndTime(),
                    savedTask.getScheduleConfig().getLastFireTime(), // 上次执行时间
                    savedTask.getScheduleConfig().getNextFireTime()); // 下次执行时间
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
                    updatedTask.getScheduleConfig().getEndTime(),
                    updatedTask.getScheduleConfig().getLastFireTime(), // 上次执行时间
                    updatedTask.getScheduleConfig().getNextFireTime()); // 下次执行时间
            return ResponseEntity.ok(ApiResponse.success(dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }

    /*
    修改定时任务状态
    */
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

   
    /*
    加载所有可选的链接
    */
    @GetMapping("/monitottask/loadOptionalUrls")
    public ResponseEntity<ApiResponse<List<ConfigItem>>> loadOptionalUrls() {
        //TODO: process POST request
        try{
            List<ConfigItem> load_list=baseUrlsManagerService.loadPotional_Urls();
            // System.out.println("加载的可选链接列表：" + load_list);
            return ResponseEntity.status(200).body(ApiResponse.success(load_list));
        }catch (Exception e) {
            // TODO: handle exception
           return ResponseEntity.status(404).body(ApiResponse.error(500, "配置文件加载错误"));
        }
    }
    


    // 获取某监控任务下的所有监控链接
    @PostMapping("/monitottask/getTaskUrlsByTaskId")
    public ResponseEntity<ApiResponse<List<MonitorUrlListDTO>>> getTaskUrlsByTaskId(@RequestBody Long TaskID) {
        List<MonitorUrl> list=monitorTaskService.findAllTaskUrlsByTaskId(TaskID);
        List<MonitorUrlListDTO> result=new ArrayList<>();
        for(MonitorUrl item : list){
            result.add(new MonitorUrlListDTO(item.getId(),item.getTask().getId(),item.getUrl(),item.isEnabled(),item.getRemark()));
        }
        return ResponseEntity.status(200).body(ApiResponse.success(result));
    }
    
    // 添加监控链接到任务
    @PostMapping("/monitottask/addLink")
    public ResponseEntity<ApiResponse<MonitorUrlListDTO>> addLinkToTask(@RequestBody AddLinkRequest request, HttpServletRequest httpRequest) {
        System.out.println("收到添加链接请求: " + (request != null ? request.toString() : "null"));
        User user = (User) httpRequest.getAttribute("userInfo");
        if (user == null) {
            System.out.println("用户未授权");
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未授权"));
        }
        
        System.out.println("当前用户ID: " + user.getId());
        
        if (request == null || request.getTaskId() == null || request.getUrl() == null) {
            System.out.println("请求数据错误: taskId=" + (request != null ? request.getTaskId() : "null") + 
                             ", url=" + (request != null ? request.getUrl() : "null"));
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "请求数据错误"));
        }
        
        try {
            // 验证任务是否存在且属于当前用户
            System.out.println("查找任务ID: " + request.getTaskId());
            MonitorTask task = monitorTaskService.getTaskById(request.getTaskId());
            if (task == null) {
                System.out.println("任务不存在: " + request.getTaskId());
                return ResponseEntity.status(404).body(ApiResponse.error(404, "任务不存在"));
            }
            
            System.out.println("找到任务: " + task.getId() + ", 任务所属用户ID: " + 
                             (task.getUser() != null ? task.getUser().getId() : "null"));
            
            if (task.getUser() == null || !task.getUser().getId().equals(user.getId())) {
                System.out.println("无权限操作此任务: 任务用户ID=" + 
                                 (task.getUser() != null ? task.getUser().getId() : "null") + 
                                 ", 当前用户ID=" + user.getId());
                return ResponseEntity.status(403).body(ApiResponse.error(403, "无权限操作此任务"));
            }
            
            // 创建新的监控链接
            MonitorUrl monitorUrl = new MonitorUrl();
            monitorUrl.setUrl(request.getUrl());
            monitorUrl.setTask(task);
            monitorUrl.setEnabled(request.isEnabled());
            monitorUrl.setRemark(request.getRemark());
            
            System.out.println("创建监控链接: " + monitorUrl);
            
            // 保存监控链接
            MonitorUrl savedUrl = monitorTaskService.save_MonitorUrl(monitorUrl);
            System.out.println("保存成功: " + savedUrl);
            
            // 返回DTO
            MonitorUrlListDTO dto = new MonitorUrlListDTO(
                savedUrl.getId(),
                savedUrl.getTask().getId(),
                savedUrl.getUrl(),
                savedUrl.isEnabled(),
                savedUrl.getRemark()
            );
            
            System.out.println("返回DTO: " + dto);
            return ResponseEntity.ok(ApiResponse.success(dto));
        } catch (Exception e) {
            System.out.println("添加链接异常: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }
    
    // 删除监控链接
    @PostMapping("/monitottask/deleteLink")
    public ResponseEntity<ApiResponse<Boolean>> deleteLink(@RequestBody Long linkId, HttpServletRequest httpRequest) {
        System.out.println("收到删除链接请求，链接ID: " + linkId);
        User user = (User) httpRequest.getAttribute("userInfo");
        if (user == null) {
            System.out.println("用户未授权");
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未授权"));
        }
        
        if (linkId == null) {
            System.out.println("链接ID不能为空");
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "链接ID不能为空"));
        }
        
        try {
            boolean deleted = monitorTaskService.deleteLinkById(linkId, user.getId());
            if (deleted) {
                System.out.println("链接删除成功: " + linkId);
                return ResponseEntity.ok(ApiResponse.success(true));
            } else {
                System.out.println("链接删除失败或无权删除: " + linkId);
                return ResponseEntity.status(403).body(ApiResponse.error(403, "链接删除失败或无权限"));
            }
        } catch (Exception e) {
            System.out.println("删除链接异常: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }
    
    // 切换链接状态（关闭/启用）
    @PostMapping("/monitottask/toggleLinkStatus")
    public ResponseEntity<ApiResponse<MonitorUrlListDTO>> toggleLinkStatus(@RequestBody Long linkId, HttpServletRequest httpRequest) {
        System.out.println("收到切换链接状态请求，链接ID: " + linkId);
        User user = (User) httpRequest.getAttribute("userInfo");
        if (user == null) {
            System.out.println("用户未授权");
            return ResponseEntity.status(401).body(ApiResponse.error(401, "未授权"));
        }
        
        if (linkId == null) {
            System.out.println("链接ID不能为空");
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "链接ID不能为空"));
        }
        
        try {
            MonitorUrl updatedLink = monitorTaskService.toggleLinkStatus(linkId, user.getId());
            if (updatedLink != null) {
                System.out.println("链接状态切换成功: " + linkId + ", 新状态: " + updatedLink.isEnabled());
                
                // 返回DTO
                MonitorUrlListDTO dto = new MonitorUrlListDTO(
                    updatedLink.getId(),
                    updatedLink.getTask().getId(),
                    updatedLink.getUrl(),
                    updatedLink.isEnabled(),
                    updatedLink.getRemark()
                );
                
                return ResponseEntity.ok(ApiResponse.success(dto));
            } else {
                System.out.println("链接状态切换失败或无权操作: " + linkId);
                return ResponseEntity.status(403).body(ApiResponse.error(403, "链接状态切换失败或无权限"));
            }
        } catch (Exception e) {
            System.out.println("切换链接状态异常: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(ApiResponse.error(400, e.getMessage()));
        }
    }
    
    // 内部类：添加链接请求
    public static class AddLinkRequest {
        private Long taskId;
        private String url;
        private String remark;
        private boolean enabled = true;
        private String classId;  // 改为 String，因为 ConfigItem 中的 class_id 是 String
        private String methodId; // 改为 String，因为 ConfigItem 中的 method_id 是 String
        private Integer urlId;   // 改为 Integer，因为 ConfigItem 中的 url_id 是 int
        
        // 无参构造函数（Jackson 需要）
        public AddLinkRequest() {}
        
        // Getters and Setters
        public Long getTaskId() { return taskId; }
        public void setTaskId(Long taskId) { this.taskId = taskId; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getClassId() { return classId; }
        public void setClassId(String classId) { this.classId = classId; }
        
        public String getMethodId() { return methodId; }
        public void setMethodId(String methodId) { this.methodId = methodId; }
        
        public Integer getUrlId() { return urlId; }
        public void setUrlId(Integer urlId) { this.urlId = urlId; }
        
        @Override
        public String toString() {
            return "AddLinkRequest{" +
                    "taskId=" + taskId +
                    ", url='" + url + '\'' +
                    ", remark='" + remark + '\'' +
                    ", enabled=" + enabled +
                    ", classId='" + classId + '\'' +
                    ", methodId='" + methodId + '\'' +
                    ", urlId=" + urlId +
                    '}';
        }
    }
}



