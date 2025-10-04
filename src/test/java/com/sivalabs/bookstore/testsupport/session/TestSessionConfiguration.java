package com.sivalabs.bookstore.testsupport.session;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.SessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;

@TestConfiguration
@EnableSpringHttpSession
public class TestSessionConfiguration {

    @Bean
    @ConditionalOnMissingBean(SessionRepository.class)
    public MapSessionRepository sessionRepository() {
        Map<String, org.springframework.session.Session> sessions = new ConcurrentHashMap<>();
        MapSessionRepository repository = new MapSessionRepository(sessions);
        repository.setDefaultMaxInactiveInterval(Duration.ofMinutes(30));
        return repository;
    }
}
