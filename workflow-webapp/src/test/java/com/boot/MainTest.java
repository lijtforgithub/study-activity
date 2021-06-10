package com.boot;

import com.boot.guowy.cloud.Main;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 测试入口
 */
@SuppressWarnings("all")
@SpringBootApplication
public class MainTest {

    public static void main(String[] args) {
        args = new String[]{"--spring.application.name=guowy-workflow-webapp"};
        Main.run(MainTest.class, args);
    }

}
