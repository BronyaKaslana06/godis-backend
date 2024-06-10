package com.example.godisConnector.controller;

import com.example.godisConnector.common.BaseResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hello")
public class HelloController {
    @GetMapping("/sayHello")
    public BaseResponse<String> userRegister() {
        return new BaseResponse<>(200, "hello software testing", "OK");
    }

}
