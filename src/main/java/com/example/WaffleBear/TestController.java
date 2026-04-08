package com.example.WaffleBear;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/test")
public class TestController {

    @GetMapping("/version")
    public void Test() {
        System.out.println("v4");
    }
}
