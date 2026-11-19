package com.readscape.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.readscape.testfixtures.KafkaContainerConfig;
import com.readscape.testfixtures.PostgresContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({PostgresContainerConfig.class, KafkaContainerConfig.class})
class NotificationApplicationTest {

    @LocalServerPort
    int port;

    @Test
    void healthIsUp() {
        String body = RestClient.create("http://localhost:" + port)
                .get().uri("/actuator/health").retrieve().body(String.class);
        assertThat(body).contains("\"status\":\"UP\"");
    }
}
