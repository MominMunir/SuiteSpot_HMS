package com.suitespot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "backup")
public class BackupProperties {
    
    /**
     * PostgreSQL dump utility configuration
     * Maps to backup.pg-dump.path or backup.pgDump.path in application.properties
     * Spring Boot relaxed binding supports: pg-dump, pgDump, pg_dump
     */
    private PgDump pgDump = new PgDump();
    
    public PgDump getPgDump() {
        return pgDump;
    }
    
    public void setPgDump(PgDump pgDump) {
        this.pgDump = pgDump;
    }
    
    public static class PgDump {
        private String path;
        
        public String getPath() {
            return path;
        }
        
        public void setPath(String path) {
            this.path = path;
        }
    }
}

