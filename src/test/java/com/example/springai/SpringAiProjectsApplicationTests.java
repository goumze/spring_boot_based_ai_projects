package com.example.springai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Basic context-load test for the Spring Boot application.
 *
 * <p>Uses a stub OpenAI API key so that the context can start without real credentials.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.ai.openai.api-key=test-key"
})
class SpringAiProjectsApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the Spring application context starts successfully
    }
}
