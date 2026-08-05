package com.unical.travelapp.backend.security.support;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Appender in-memory per catturare gli eventi di un logger durante un test.
 *
 * <p>Usa il ListAppender di Logback gia' presente sul classpath invece di introdurre
 * una libreria esterna di log-capture.
 */
public final class CatturaLog implements AutoCloseable {

    private final Logger logger;
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    private CatturaLog(String nomeLogger) {
        this.logger = (Logger) LoggerFactory.getLogger(nomeLogger);
        this.appender.start();
        this.logger.addAppender(appender);
    }

    public static CatturaLog di(String nomeLogger) {
        return new CatturaLog(nomeLogger);
    }

    public static CatturaLog audit() {
        return new CatturaLog("AUDIT");
    }

    public static CatturaLog root() {
        return new CatturaLog(org.slf4j.Logger.ROOT_LOGGER_NAME);
    }

    public List<String> righe() {
        return appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }

    public String testoCompleto() {
        return appender.list.stream()
                .map(evento -> evento.getFormattedMessage()
                        + (evento.getThrowableProxy() != null ? " " + evento.getThrowableProxy().getMessage() : ""))
                .collect(Collectors.joining("\n"));
    }

    @Override
    public void close() {
        logger.detachAppender(appender);
        appender.stop();
    }
}
