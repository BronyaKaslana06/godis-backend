package com.example.godisConnector.controller;

import com.example.godisConnector.common.BaseResponse;
import com.example.godisConnector.common.ErrorCode;
import com.example.godisConnector.dto.CommandDTO;
import com.example.godisConnector.dto.ServerDTO;
import com.example.godisConnector.dto.StressTestDTO;

import io.swagger.models.auth.In;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
public class RedisController {

    private Jedis jedis = new Jedis("localhost", 6389); // 根据你的 Redis 服务器地址进行调整
    // private Jedis jedis; // 初始化为空

    @PostMapping("/execute")
    public BaseResponse<Object> executeCommand(@RequestBody CommandDTO commandDto) {
        List<String> parts = splitCommand(commandDto.getCommand());
        System.out.println(parts);
        String commandName = parts.get(0).toUpperCase();
        String[] args = parts.subList(1, parts.size()).toArray(new String[0]);

        try {
            // 检查并发送命令，包括自定义命令
            Protocol.Command cmd = null;

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

            if (result == null) {
                return new BaseResponse<>(ErrorCode.GET_NIL.getCode(), "nil", ErrorCode.GET_NIL.getMessage());
            }

            // 特殊错误处理
            if (result.toString().equals("Error handling response for command: ERR command unknown")) {
                return new BaseResponse<>(ErrorCode.COMMAND_UNKNOWN.getCode(),
                        "Error executing command: " + commandName, result.toString());
            } else if (result.toString().equals("Error executing command: ERR not enough args")) {
                return new BaseResponse<>(ErrorCode.PARAMS_ERROR.getCode(), "Error executing command: " + commandName,
                        result.toString());
            } else if (result.toString().equals("(Err) Only one parameter is allowed.")) {
                return new BaseResponse<>(ErrorCode.PARAMS_ERROR.getCode(), "Error executing command: " + commandName,
                        result.toString());
            }

            // 返回成功响应
            return new BaseResponse<>(ErrorCode.SUCCESS.getCode(), result, ErrorCode.SUCCESS.getMessage());
        } catch (Exception e) {
            if (e.getMessage().equals("ERR not enough args")) {
                return new BaseResponse<>(ErrorCode.PARAMS_ERROR.getCode(), "Error executing command: " + commandName,
                        ErrorCode.PARAMS_ERROR.getMessage());
            }
            // 返回未知错误响应
            return new BaseResponse<>(ErrorCode.UNKNOWN_ERR.getCode(), "Error executing command: " + e.getMessage(),
                    ErrorCode.UNKNOWN_ERR.getMessage());
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

    private final AtomicInteger currentConnections = new AtomicInteger(0);
    private final AtomicInteger successfulCommands = new AtomicInteger(0);
    private final AtomicInteger failedCommands = new AtomicInteger(0);
    private volatile boolean isTestRunning = false;
    private ExecutorService executor;

    @PostMapping("/startStressTest")
    public BaseResponse<String> startStressTest() {
        if (isTestRunning) {
            return new BaseResponse<>(400, "Test is already running", "Error");
        }

        isTestRunning = true;
        currentConnections.set(0);
        successfulCommands.set(0);
        failedCommands.set(0);

        executor = Executors.newCachedThreadPool();
        executor.submit(this::addConnections);

        return new BaseResponse<>(200, "Stress test started", "Success");
    }

    @GetMapping("/getTestStatus")
    public BaseResponse<StressTestDTO> getTestStatus() {
        if (!isTestRunning) {
            return new BaseResponse<>(400, null, "No test is running");
        }

        StressTestDTO status = new StressTestDTO(
                currentConnections.get(),
                successfulCommands.get(),
                failedCommands.get());

        return new BaseResponse<>(200, status, "Success");
    }

    @PostMapping("/stopStressTest")
    public BaseResponse<String> stopStressTest() {
        if (!isTestRunning) {
            return new BaseResponse<>(400, "No test is running", "Error");
        }

        isTestRunning = false;
        executor.shutdownNow();

        return new BaseResponse<>(200, "Stress test stopped", "Success");
    }

    private void addConnections() {
        while (isTestRunning) {
            executor.submit(this::runConnection);
            currentConnections.incrementAndGet();
            try {
                Thread.sleep(1); // 每1ms添加一个新连接
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void runConnection() {
        Jedis jedis = new Jedis("localhost", 6389); // 使用实际的地址和端口
        try {
            while (isTestRunning) {
                try {
                    jedis.get("test_key");
                    successfulCommands.incrementAndGet();
                } catch (Exception e) {
                    failedCommands.incrementAndGet();
                }
                Thread.sleep(500); // 每500ms执行一次GET操作
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            jedis.close();
        }
    }

    private Object handleReply(String commandName, Protocol.Command cmd) throws UnsupportedEncodingException {
        switch (commandName) {
            case "ZADD":
                // return jedis.getClient().getIntegerReply();
            case "LPUSH":
            case "RPUSH":
            case "ZCARD":
            case "ZCOUNT":
                return jedis.getClient().getIntegerReply(); // 返回列表的新长度
            case "LPOP":
            case "RPOP":
                return jedis.getClient().getBulkReply(); // 返回元素的值
            case "ZRANGE":
                List<Object> response = jedis.getClient().getObjectMultiBulkReply();
                Map<String, Double> resultMap = new HashMap<>();
                String key = null;
                for (Object item : response) {
                    if (item instanceof byte[]) {
                        // 将字节数组转换为字符串
                        // System.out.println(new String((byte[]) item, "UTF-8"));
                        key = new String((byte[]) item, "UTF-8");
                    } else {
                        // 直接打印数字
                        // System.out.println(item);
                        if (key != null) {
                            resultMap.put(key, (Double) item);
                            key = null; // 重置键，以防不是成对出现
                        }
                    }
                }
                return resultMap;
            case "ZINCRBY":
                Object res = jedis.getClient().getOne();
                // System.out.println(res.toString());
                return (Double) res;
            case "SISMEMBER":
            case "SADD":
            case "ZREM":
            case "SCARD":
            case "SREM":
                Object remRes = jedis.getClient().getOne();
                System.out.println(remRes.toString());
                return remRes;
            case "SMEMBERS":
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
