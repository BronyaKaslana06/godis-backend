package com.example.godisConnector.dto;

public class StressTestDTO {
    private int maxConnections;
    private int incrementStep;
    private String command;
    private long testDurationSeconds;
    private String serverUrl;
    private int port;

    
    public int getMaxConnections() {
        return maxConnections;
    }
    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }
    public int getIncrementStep() {
        return incrementStep;
    }
    public void setIncrementStep(int incrementStep) {
        this.incrementStep = incrementStep;
    }
    public String getCommand() {
        return command;
    }
    public void setCommand(String command) {
        this.command = command;
    }
    public long getTestDurationSeconds() {
        return testDurationSeconds;
    }
    public void setTestDurationSeconds(long testDurationSeconds) {
        this.testDurationSeconds = testDurationSeconds;
    }
    public String getServerUrl() {
        return serverUrl;
    }
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }
    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }

    // Getters and setters
    // ...
}