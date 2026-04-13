package com.shixin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Root Controller
 * Handles root path access and redirects to login page
 */
@Controller
public class RootController {

    /**
     * Handle root path ("/") access
     * Redirect to login page
     * @return redirect to login page
     */
    @GetMapping("/")
    public String redirectToLogin() {
        // Redirect to login page
        return "redirect:/html/login.html";
    }

    /**
     * Handle root path ("/index") access
     * Also redirect to login page
     * @return redirect to login page
     */
    @GetMapping("/index")
    public String redirectIndexToLogin() {
        // Redirect to login page
        return "redirect:/html/login.html";
    }

    /**
     * Handle root path ("/home") access
     * Also redirect to login page
     * @return redirect to login page
     */
    @GetMapping("/home")
    public String redirectHomeToLogin() {
        // Redirect to login page
        return "redirect:/html/login.html";
    }
}