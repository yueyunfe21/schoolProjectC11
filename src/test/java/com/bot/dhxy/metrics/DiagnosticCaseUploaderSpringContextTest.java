package com.bot.dhxy.metrics;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

public class DiagnosticCaseUploaderSpringContextTest {

    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test-case-upload", Map.of(
                    "case.upload.enabled", "false",
                    "case.upload.endpoint", "",
                    "case.upload.token", "",
                    "case.upload.connect-timeout-ms", "100",
                    "case.upload.read-timeout-ms", "100",
                    "case.upload.max-attempts", "1",
                    "case.upload.retry-delay-ms", "100"
            )));
            context.register(DiagnosticCaseUploaderService.class);
            context.refresh();
            context.getBean(DiagnosticCaseUploaderService.class);
        }
    }
}
