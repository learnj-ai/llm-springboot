package com.techcorp.assistant.module06;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class Module06Application {

    public static void main(String[] args) {
        SpringApplication.run(Module06Application.class, args);
    }
}
