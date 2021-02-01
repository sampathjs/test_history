package com.matthey.pmm;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

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
    
    static <T> T runScriptWithLogging(String scriptCategory, Class<?> clazz, Logger logger, Supplier<T> supplier) {
        try {
            EndurLoggerFactory.configureLogLocation(getRootDir(), clazz);
            String className = clazz.getName();
            logger.info("{} {} has started", scriptCategory, className);
            T result = supplier.get();
            logger.info("{} {} has ended", scriptCategory, className);
            return result;
        } catch (Throwable e) {
            logger.error("error occurred: {}", e.getMessage(), e);
            throw e;
        }
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
    
    private static Path getRootDir() {
        String root = System.getenv("AB_OUTDIR");
        return Paths.get(root, "logs");
    }
    
    static void runScriptWithLogging(String scriptCategory, Class<?> clazz, Logger logger, Runnable runnable) {
        try {
            EndurLoggerFactory.configureLogLocation(getRootDir(), clazz);
            String className = clazz.getName();
            logger.info("{} {} has started", scriptCategory, className);
            runnable.run();
            logger.info("{} {} has ended", scriptCategory, className);
        } catch (Throwable e) {
            logger.error("error occurred: {}", e.getMessage(), e);
            throw e;
        }
    }
}
