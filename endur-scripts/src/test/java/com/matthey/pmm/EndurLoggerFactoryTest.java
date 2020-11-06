package com.matthey.pmm;

import ch.qos.logback.classic.Logger;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class EndurLoggerFactoryTest {
    
    @TempDir
    Path outDir;
    private Logger logger;
    
    @BeforeEach
    public void setUp() {
        logger = EndurLoggerFactory.getLogger(EndurLoggerFactoryTest.class);
    }
    
    @AfterEach
    public void tearDown() {
        // logger is created purely for the purpose of stopping loggerContext
        // otherwise jUnit cannot delete the temporary folder because the log files are still in use
        logger.getLoggerContext().stop();
    }
    
    // this mimics different scripts being loaded into the same script engine
    @Test
    public void change_log_location_programmatically() throws IOException {
        ClassC classC = new ClassC();
        new ClassA().execute();
        classC.execute("classA");
        new ClassB().execute();
        classC.execute("classB");
        
        Path firstLog = outDir.resolve("ClassA.log");
        assertThat(Files.exists(firstLog)).isTrue();
        String firstLogContent = getLog(firstLog);
        assertThat(firstLogContent).containsOnlyOnce("test logging for ClassA");
        assertThat(firstLogContent).containsOnlyOnce("test logging for ClassC in classA");
        assertThat(firstLogContent).doesNotContain("test logging for ClassB");
        
        Path secondLog = outDir.resolve("ClassB.log");
        assertThat(Files.exists(secondLog)).isTrue();
        String secondLogContent = getLog(secondLog);
        assertThat(secondLogContent).containsOnlyOnce("test logging for ClassB");
        assertThat(secondLogContent).containsOnlyOnce("test logging for ClassC in classB");
        assertThat(secondLogContent).doesNotContain("test logging for ClassA");
    }
    
    private static String getLog(Path path) throws IOException {
        return FileUtils.readFileToString(path.toFile(), Charset.defaultCharset());
    }
    
    private static class ClassC {
        
        private final Logger logger = EndurLoggerFactory.getLogger(ClassC.class);
        
        protected void execute(String extra) {
            logger.info("test logging for ClassC in " + extra);
        }
    }
    
    private class ClassA {
        
        private final Logger logger = EndurLoggerFactory.getLogger(ClassA.class);
        
        protected void execute() {
            EndurLoggerFactory.configureLogLocation(outDir, ClassA.class);
            logger.info("test logging for ClassA");
        }
    }
    
    private class ClassB {
        
        private final Logger logger = EndurLoggerFactory.getLogger(ClassB.class);
        
        protected void execute() {
            EndurLoggerFactory.configureLogLocation(outDir, ClassB.class);
            logger.info("test logging for ClassB");
        }
    }
}