package com.example.godisConnector.dto;

public class StressTestDTO {
    public int currentConnections;
    public int successfulCommands;
    public int failedCommands;

    public StressTestDTO(int currentConnections, int successfulCommands, int failedCommands) {
        this.currentConnections = currentConnections;
        this.successfulCommands = successfulCommands;
        this.failedCommands = failedCommands;
    }
}