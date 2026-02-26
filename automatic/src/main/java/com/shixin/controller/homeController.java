package com.shixin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shixin.service.ServerStatusService;


@RestController
public class HomeController {

    @GetMapping("/home")
    public String getMethodName() {
        return "Hello World!";
    }

    @Autowired 
    private ServerStatusService serverStatusService;
    @GetMapping("/service/status")
    public String getStatus() {
        return serverStatusService.getStatus();
    }
    
    
}
