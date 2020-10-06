package com.matthey.pmm;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;

import java.nio.file.Path;

public class EndurLoggerFactory {
    
    private static final LoggerContext loggerContext = createLoggerContext();
    
    public static Logger getLogger(Class<?> clazz) {
        return loggerContext.getLogger(clazz);
    }
    
    public static LoggerContext createLoggerContext() {
        LoggerContext loggerContext = new LoggerContext();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(loggerContext);
        try {
            configurator.doConfigure(EndurLoggerFactory.class.getResourceAsStream("/logback.xml"));
        } catch (Exception e) {
            throw new RuntimeException("failed to load logback configuration: " + e.getMessage(), e);
        }
        return loggerContext;
    }
    
    public static void configureLogLocation(Path rootDir, Class<?> clazz) {
        Logger logger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        RollingFileAppender<ILoggingEvent> appender = (RollingFileAppender<ILoggingEvent>) logger.getAppender("File");
        appender.stop();
        String logPath = getLogPath(rootDir, clazz);
        appender.setFile(logPath);
        ((FixedWindowRollingPolicy) appender.getRollingPolicy()).setFileNamePattern(logPath + ".%i.zip");
        appender.getTriggeringPolicy().start();
        appender.start();
    }
    
    private static String getLogPath(Path rootDir, Class<?> clazz) {
        String className = clazz.getSimpleName();
        return rootDir.resolve(className + ".log").toString();
    }
}
