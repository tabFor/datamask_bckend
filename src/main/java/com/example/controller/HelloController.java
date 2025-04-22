package com.example.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "系统测试", description = "系统测试和健康检查接口")
public class HelloController {
    
    @Operation(
        summary = "系统测试接口", 
        description = "返回简单的问候信息，用于测试系统是否正常运行"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "系统正常运行"
    )
    @GetMapping("/")
    public String hello() {
        return "Hello, Spring Boot!";
    }
}