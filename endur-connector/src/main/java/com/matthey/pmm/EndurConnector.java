package com.matthey.pmm;

import com.olf.openrisk.application.Application;
import com.olf.openrisk.application.EnumMessageSeverity;
import com.olf.openrisk.application.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication(scanBasePackages = "com.matthey.pmm")
@EnableSwagger2
public class EndurConnector {

    private static final Logger logger = LogManager.getLogger(EndurConnector.class);
    private static Session session;

    public static void main(Session session) {
        logger.info("Endur Connector started with session {}", session.getSessionId());
        EndurConnector.session = session;
        main(new String[]{});
    }

    public static void main(String[] args) {
    	logger.info("EndurConnector main method");
    	SpringApplication.run(EndurConnector.class, args);
    }

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2).select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build();
    }

    @Bean
    public Session session() {
        session = session == null ? Application.getInstance().attach() : session;
        logger.info("associated with Endur: session id -> {}; process id -> {}; host -> {}",
                    session.getSessionId(),
                    session.getProcessId(),
                    session.getHostName());
        return session == null ? Application.getInstance().attach() : session;
    }
}
