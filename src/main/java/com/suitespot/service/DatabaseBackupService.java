package com.suitespot.service;

import com.suitespot.config.BackupProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class DatabaseBackupService {

    @Value("${spring.datasource.url:jdbc:postgresql://localhost:6969/suitespot}")
    private String jdbcUrl;

    @Value("${spring.datasource.username:postgres}")
    private String username;

    @Value("${spring.datasource.password:}")
    private String password;

    @Autowired(required = false)
    private BackupProperties backupProperties;

    /**
     * Create a database backup
     * @param backupLocation Directory where backup should be saved
     * @param backupType "full" for full backup, "incremental" for incremental (currently both do full)
     * @return Path to the backup file
     * @throws IOException if backup fails
     */
    public String createBackup(String backupLocation, String backupType) throws IOException {
        // Extract database name from JDBC URL
        String dbName = extractDatabaseName(jdbcUrl);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String backupFileName = String.format("%s_%s_%s.sql", dbName, backupType, timestamp);
        
        Path backupDir = Paths.get(backupLocation);
        if (!Files.exists(backupDir)) {
            Files.createDirectories(backupDir);
        }
        
        Path backupFile = backupDir.resolve(backupFileName);
        
        // Find pg_dump executable
        String pgDumpExecutable = findPgDump();
        if (pgDumpExecutable == null) {
            throw new IOException(
                "pg_dump not found. Please ensure PostgreSQL is installed and pg_dump is in your system PATH, " +
                "or set 'backup.pg-dump.path' in application.properties to the full path to pg_dump.exe. " +
                "Common locations: C:\\Program Files\\PostgreSQL\\<version>\\bin\\pg_dump.exe"
            );
        }
        
        // Extract host and port from JDBC URL
        String host = extractHost(jdbcUrl);
        String port = extractPort(jdbcUrl);
        
        // Execute pg_dump command
        ProcessBuilder processBuilder = new ProcessBuilder(
            pgDumpExecutable,
            "-h", host,
            "-p", port,
            "-U", username,
            "-d", dbName,
            "-F", "c", // Custom format (compressed)
            "-f", backupFile.toString()
        );
        
        processBuilder.environment().put("PGPASSWORD", password);
        
        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("Cannot run program")) {
                throw new IOException(
                    "Failed to execute pg_dump. Please ensure PostgreSQL is installed and pg_dump is accessible. " +
                    "Error: " + e.getMessage(), e
                );
            }
            throw e;
        }
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
             BufferedReader errorReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream()))) {
            
            String line;
            StringBuilder errorOutput = new StringBuilder();
            
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                throw new IOException("Backup failed: " + errorOutput.toString());
            }
            
            // Verify backup file exists and is not empty
            if (!Files.exists(backupFile) || Files.size(backupFile) == 0) {
                throw new IOException("Backup file was not created or is empty");
            }
            
            return backupFile.toString();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Backup process was interrupted", e);
        }
    }
    
    /**
     * Find pg_dump executable path
     */
    private String findPgDump() {
        // If path is configured, use it
        if (backupProperties != null && backupProperties.getPgDump() != null) {
            String configuredPath = backupProperties.getPgDump().getPath();
            if (configuredPath != null && !configuredPath.trim().isEmpty()) {
                Path path = Paths.get(configuredPath);
                if (Files.exists(path) && Files.isExecutable(path)) {
                    return path.toString();
                }
            }
        }
        
        // Try common PostgreSQL installation paths on Windows
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            String[] commonPaths = {
                "C:\\Program Files\\PostgreSQL\\18\\bin\\pg_dump.exe",
                "C:\\Program Files\\PostgreSQL\\17\\bin\\pg_dump.exe",
                "C:\\Program Files\\PostgreSQL\\16\\bin\\pg_dump.exe",
                "C:\\Program Files\\PostgreSQL\\15\\bin\\pg_dump.exe",
                "C:\\Program Files\\PostgreSQL\\14\\bin\\pg_dump.exe",
                "C:\\Program Files\\PostgreSQL\\13\\bin\\pg_dump.exe",
                "C:\\Program Files (x86)\\PostgreSQL\\18\\bin\\pg_dump.exe",
                "C:\\Program Files (x86)\\PostgreSQL\\17\\bin\\pg_dump.exe",
                "C:\\Program Files (x86)\\PostgreSQL\\16\\bin\\pg_dump.exe",
                "C:\\Program Files (x86)\\PostgreSQL\\15\\bin\\pg_dump.exe",
                "C:\\Program Files (x86)\\PostgreSQL\\14\\bin\\pg_dump.exe",
                "C:\\Program Files (x86)\\PostgreSQL\\13\\bin\\pg_dump.exe"
            };
            
            for (String path : commonPaths) {
                Path pgDumpPath = Paths.get(path);
                if (Files.exists(pgDumpPath)) {
                    return path;
                }
            }
        }
        
        // Try to find in PATH
        try {
            ProcessBuilder pb = new ProcessBuilder("pg_dump", "--version");
            Process p = pb.start();
            int exitCode = p.waitFor();
            if (exitCode == 0) {
                return "pg_dump";
            }
        } catch (Exception e) {
            // pg_dump not in PATH
        }
        
        return null;
    }
    
    /**
     * Extract host from JDBC URL
     */
    private String extractHost(String jdbcUrl) {
        // Format: jdbc:postgresql://host:port/database
        int start = jdbcUrl.indexOf("//") + 2;
        int end = jdbcUrl.indexOf(":", start);
        if (end == -1) {
            end = jdbcUrl.indexOf("/", start);
        }
        if (end != -1 && end > start) {
            return jdbcUrl.substring(start, end);
        }
        return "localhost";
    }
    
    /**
     * Extract port from JDBC URL
     */
    private String extractPort(String jdbcUrl) {
        // Format: jdbc:postgresql://host:port/database
        int start = jdbcUrl.indexOf("//") + 2;
        int colonIndex = jdbcUrl.indexOf(":", start);
        if (colonIndex != -1) {
            int end = jdbcUrl.indexOf("/", colonIndex);
            if (end != -1 && end > colonIndex + 1) {
                return jdbcUrl.substring(colonIndex + 1, end);
            }
        }
        return "5432"; // Default PostgreSQL port
    }
    
    /**
     * Verify backup integrity by checking file existence and size
     */
    public boolean verifyBackup(String backupFilePath) {
        try {
            Path backupPath = Paths.get(backupFilePath);
            if (!Files.exists(backupPath)) {
                return false;
            }
            
            long fileSize = Files.size(backupPath);
            // Backup file should be at least 1KB
            return fileSize > 1024;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Extract database name from JDBC URL
     */
    private String extractDatabaseName(String jdbcUrl) {
        // Format: jdbc:postgresql://host:port/database
        int lastSlash = jdbcUrl.lastIndexOf('/');
        if (lastSlash != -1 && lastSlash < jdbcUrl.length() - 1) {
            String dbPart = jdbcUrl.substring(lastSlash + 1);
            // Remove query parameters if any
            int questionMark = dbPart.indexOf('?');
            if (questionMark != -1) {
                return dbPart.substring(0, questionMark);
            }
            return dbPart;
        }
        return "suitespot"; // Default
    }
    
    /**
     * List all backup files in a directory
     */
    public File[] listBackups(String backupLocation) {
        File backupDir = new File(backupLocation);
        if (!backupDir.exists() || !backupDir.isDirectory()) {
            return new File[0];
        }
        
        return backupDir.listFiles((dir, name) -> 
            name.endsWith(".sql") || name.endsWith(".backup")
        );
    }
}

