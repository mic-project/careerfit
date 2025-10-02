package com.codelab.micproject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MicProjectApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    @DisplayName("OPENVIDU TEST")

    void openviduTest() {
        System.out.println("OPENVIDU TEST");

    }

}
