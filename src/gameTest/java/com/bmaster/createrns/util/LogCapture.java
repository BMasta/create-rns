package com.bmaster.createrns.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.util.ArrayList;
import java.util.List;

public class LogCapture implements AutoCloseable {
    private final Logger logger;
    private final Appender appender;
    private final List<String> messages = new ArrayList<>();

    private LogCapture(String loggerName) {
        logger = (Logger) LogManager.getLogger(loggerName);
        appender = new AbstractAppender("create-rns-gametest-log-capture", null, PatternLayout.createDefaultLayout(),
                false, Property.EMPTY_ARRAY) {
            @Override
            public void append(LogEvent event) {
                messages.add(event.getMessage().getFormattedMessage());
            }
        };
        appender.start();
        logger.addAppender(appender);
    }

    public static LogCapture capture(String loggerName) {
        return new LogCapture(loggerName);
    }

    public boolean contains(String fragment) {
        return messages.stream().anyMatch(message -> message.contains(fragment));
    }

    @Override
    public void close() {
        logger.removeAppender(appender);
        appender.stop();
    }
}
