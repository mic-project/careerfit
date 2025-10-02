package com.codelab.micproject.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

@Slf4j
@Component
public class DataSourceProbe {
    private final DataSource ds;
    public DataSourceProbe(DataSource ds) { this.ds = ds; }

    @EventListener(ApplicationReadyEvent.class)
    public void logDsInfo() throws Exception {
        try (Connection c = ds.getConnection()) {
            log.info("JDBC URL = {}", c.getMetaData().getURL());
            log.info("DB User  = {}", c.getMetaData().getUserName());
            try (ResultSet rs = c.createStatement().executeQuery("SELECT DATABASE()")) {
                if (rs.next()) log.info("Current schema = {}", rs.getString(1));
            }
        }
    }
}
