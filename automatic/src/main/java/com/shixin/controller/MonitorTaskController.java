package com.shixin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.shixin.serviceimpl.MonitorTaskService;




@RestController
public class MonitorTaskController {

    @Autowired
    private MonitorTaskService monitorTaskService;

    @GetMapping("/monitottask/getall")
    public String getMethodName() {
        return monitorTaskService.getAllTasks().toString();
    }
    
    @GetMapping("/monitottask/updatekeywords/{id}/{keywords}")
    public String updateTaskKeywords(@PathVariable long id, @PathVariable String keywords) {
        int result = monitorTaskService.updateTaskKeywords(id, keywords);
        if (result > 0) {
            return "Keywords updated successfully";
        } else {    
            return "Failed to update keywords";
        }
    }
        
}
