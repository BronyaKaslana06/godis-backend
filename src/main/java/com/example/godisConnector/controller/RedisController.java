package com.example.godisConnector.controller;

import com.example.godisConnector.common.BaseResponse;
import com.example.godisConnector.dto.CommandDTO;
import com.example.godisConnector.dto.ServerDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.commands.ProtocolCommand;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
public class RedisController {

    private Jedis jedis = new Jedis("localhost", 6389); // 根据你的 Redis 服务器地址进行调整
//    private Jedis jedis; // 初始化为空

    @PostMapping("/execute")
    public BaseResponse<Object> executeCommand(@RequestBody CommandDTO commandDto) {
        try {
            List<String> parts = splitCommand(commandDto.getCommand());
            System.out.println(parts);

            String commandName = parts.get(0).toUpperCase();
            String[] args = parts.subList(1, parts.size()).toArray(new String[0]);

            // 检查并发送命令，包括自定义命令
            Protocol.Command cmd = null;
//            boolean isCommandSupported = true;

            try {
                cmd = Protocol.Command.valueOf(commandName); // 尝试找到内置命令
                jedis.getClient().sendCommand(cmd, args); // 发送内置命令
            } catch (IllegalArgumentException e) {
                // 命令不是 Jedis 内置的，可能是自定义的
                jedis.getClient().sendCommand(new ProtocolCommand() {
                    @Override
                    public byte[] getRaw() {
                        return commandName.getBytes(StandardCharsets.UTF_8);
                    }
                }, args); // 直接使用命令名发送自定义命令
            }

            Object result = handleReply(commandName, cmd);

            // 返回成功响应
            return new BaseResponse<>(200, result, "Success");
        } catch (Exception e) {
            // 返回错误响应
            return new BaseResponse<>(500, "Error executing command: " + e.getMessage());
        }
    }

    @PostMapping("/connect")
    public BaseResponse<String> connectToServer(@RequestBody ServerDTO serverDto) {
        System.out.println(serverDto.toString());
        try {
            if (jedis != null) {
                jedis.close(); // 关闭旧的连接
            }
            // 创建新的连接
            jedis = new Jedis(serverDto.getServerUrl(), serverDto.getPort());
            jedis.connect();
            if (jedis.isConnected()) {
                return new BaseResponse<>(200, "Connected successfully", "Success");
            } else {
                return new BaseResponse<>(500, "Failed to connect", "Failure");
            }
        } catch (Exception e) {
            return new BaseResponse<>(500, "Error connecting to server: " + e.getMessage(), "Failure");
        }
    }


    private Object handleReply(String commandName, Protocol.Command cmd) {
        switch (commandName) {
            case "LPUSH":
            case "RPUSH":
                return jedis.getClient().getIntegerReply(); // 返回列表的新长度
            case "LPOP":
            case "RPOP":
                return jedis.getClient().getBulkReply(); // 返回元素的值
            case "LRANGE":
                return jedis.getClient().getMultiBulkReply(); // 返回元素列表
            case "MATCHKEY":
            case "GETALLKEYS":
                return jedis.getClient().getBulkReply(); // 假设这些命令返回列表
            default:
                try {
                    return jedis.getClient().getBulkReply(); // 默认尝试返回字符串
                } catch (Exception e) {
                    return "Error handling response for command: " + e.getMessage();
                }
        }
    }

    private List<String> splitCommand(String command) {
        List<String> tokens = new ArrayList<>();
        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(command);
        while (m.find()) {
            tokens.add(m.group(1).replace("\"", "")); // Remove surrounding quotes
        }
        return tokens;
    }
}
