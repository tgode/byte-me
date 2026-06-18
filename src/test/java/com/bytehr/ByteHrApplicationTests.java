package com.bytehr;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "sharepoint.sync-enabled=false",
        "ollama.base-url=http://localhost:11434",
        "teams.app-id=test",
        "teams.app-password=test"
})
class ByteHrApplicationTests {

    @Test
    void contextLoads() {
    }
}
