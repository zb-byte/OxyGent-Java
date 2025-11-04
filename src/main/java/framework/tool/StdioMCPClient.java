package framework.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import framework.model.AgentRequest;
import framework.model.AgentResponse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Stdio MCP 客户端（框架核心）
 * 
 * 通过标准输入输出与 MCP 服务器进程通信
 * 类似于 Python 版本的 StdioMCPClient
 */
public class StdioMCPClient implements MCPClient {
    private final String name;
    private final String description;
    private final Map<String, Object> params;
    
    private Process mcpProcess;
    private BufferedReader reader;
    private PrintWriter writer;
    private final ObjectMapper objectMapper;
    private List<MCPToolInfo> tools;
    
    public StdioMCPClient(String name, String description, Map<String, Object> params) {
        this.name = name;
        this.description = description;
        this.params = params != null ? params : new HashMap<>();
        this.objectMapper = new ObjectMapper();
        this.tools = new ArrayList<>();
    }
    
    @Override
    public void initialize() throws Exception {
        System.out.println("🔧 初始化 MCP 客户端: " + name);
        
        // 构建命令
        String command = (String) params.get("command");
        @SuppressWarnings("unchecked")
        List<String> args = (List<String>) params.get("args");
        
        if (command == null || args == null) {
            throw new IllegalArgumentException("MCP 客户端参数不完整: 需要 command 和 args");
        }
        
        // 处理特殊命令（如 npx）
        if ("npx".equals(command)) {
            command = findNpxCommand();
        }
        
        // 构建完整命令列表
        List<String> commandList = new ArrayList<>();
        commandList.add(command);
        commandList.addAll(args);
        
        // 启动 MCP 服务器进程
        ProcessBuilder pb = new ProcessBuilder(commandList);
        
        // 设置环境变量
        @SuppressWarnings("unchecked")
        Map<String, String> env = (Map<String, String>) params.get("env");
        if (env != null) {
            pb.environment().putAll(env);
        }
        
        mcpProcess = pb.start();
        
        // 获取输入输出流
        reader = new BufferedReader(
            new InputStreamReader(mcpProcess.getInputStream(), StandardCharsets.UTF_8)
        );
        writer = new PrintWriter(
            new OutputStreamWriter(mcpProcess.getOutputStream(), StandardCharsets.UTF_8),
            true
        );
        
        // 初始化 MCP 协议（简化版本）
        // 在实际实现中，这里需要按照 MCP 协议进行握手
        initializeMCPProtocol();
        
        // 列出可用工具
        listTools();
        
        System.out.println("✅ MCP 客户端初始化成功: " + name);
        System.out.println("   可用工具: " + tools.size() + " 个");
    }
    
    /**
     * 初始化 MCP 协议（简化版本）
     * 
     * 注意：这是一个简化的实现。完整的 MCP 协议需要：
     * 1. 发送 initialize 请求
     * 2. 接收 initialize 响应
     * 3. 处理 JSON-RPC 协议
     */
    private void initializeMCPProtocol() throws Exception {
        // 发送初始化请求（JSON-RPC 格式）
        Map<String, Object> initRequest = new HashMap<>();
        initRequest.put("jsonrpc", "2.0");
        initRequest.put("id", 1);
        initRequest.put("method", "initialize");
        initRequest.put("params", new HashMap<>());
        
        String requestJson = objectMapper.writeValueAsString(initRequest);
        writer.println(requestJson);
        writer.flush();
        
        // 等待响应（简化版本，实际需要完整的 JSON-RPC 处理）
        Thread.sleep(500); // 给服务器时间响应
    }
    
    @Override
    public List<MCPToolInfo> listTools() throws Exception {
        // 发送 list_tools 请求
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", 2);
        request.put("method", "tools/list");
        request.put("params", new HashMap<>());
        
        String requestJson = objectMapper.writeValueAsString(request);
        writer.println(requestJson);
        writer.flush();
        
        // 读取响应（简化版本）
        // 在实际实现中，需要完整的 JSON-RPC 响应解析
        String response = readMCPResponse();
        
        // 解析工具列表（简化版本）
        // 在实际实现中，需要解析完整的 MCP 响应格式
        tools = parseToolsList(response);
        
        return tools;
    }
    
    /**
     * 读取 MCP 响应（简化版本）
     */
    private String readMCPResponse() throws Exception {
        StringBuilder sb = new StringBuilder();
        long startTime = System.currentTimeMillis();
        long timeout = 5000; // 5秒超时
        
        while (System.currentTimeMillis() - startTime < timeout) {
            if (reader.ready()) {
                String line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    sb.append(line);
                    break; // 简化版本，只读一行
                }
            }
            Thread.sleep(100);
        }
        
        return sb.toString();
    }
    
    /**
     * 解析工具列表（简化版本）
     */
    @SuppressWarnings("unchecked")
    private List<MCPToolInfo> parseToolsList(String response) {
        List<MCPToolInfo> toolList = new ArrayList<>();
        
        try {
            Map<String, Object> jsonResponse = objectMapper.readValue(response, Map.class);
            Map<String, Object> result = (Map<String, Object>) jsonResponse.get("result");
            
            if (result != null) {
                List<Map<String, Object>> toolsData = (List<Map<String, Object>>) result.get("tools");
                if (toolsData != null) {
                    for (Map<String, Object> toolData : toolsData) {
                        String toolName = (String) toolData.get("name");
                        String toolDesc = (String) toolData.get("description");
                        Map<String, Object> inputSchema = (Map<String, Object>) toolData.get("inputSchema");
                        
                        toolList.add(new MCPToolInfo(toolName, toolDesc, inputSchema));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️  解析 MCP 工具列表失败，使用模拟工具: " + e.getMessage());
            // 如果解析失败，创建一个模拟工具
            toolList.add(new MCPToolInfo("mcp_tool", "MCP 工具（模拟）", new HashMap<>()));
        }
        
        return toolList;
    }
    
    @Override
    public CompletableFuture<AgentResponse> callTool(String toolName, Map<String, Object> arguments) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 发送工具调用请求（JSON-RPC 格式）
                Map<String, Object> request = new HashMap<>();
                request.put("jsonrpc", "2.0");
                request.put("id", System.currentTimeMillis());
                request.put("method", "tools/call");
                
                Map<String, Object> params = new HashMap<>();
                params.put("name", toolName);
                params.put("arguments", arguments);
                request.put("params", params);
                
                String requestJson = objectMapper.writeValueAsString(request);
                writer.println(requestJson);
                writer.flush();
                
                // 读取响应
                String response = readMCPResponse();
                
                // 解析响应
                return parseToolResponse(response);
                
            } catch (Exception e) {
                System.err.println("❌ MCP 工具调用失败: " + e.getMessage());
                e.printStackTrace();
                return new AgentResponse(
                    "MCP 工具调用失败: " + e.getMessage(),
                    false,
                    new ArrayList<>()
                );
            }
        });
    }
    
    /**
     * 解析工具响应（简化版本）
     */
    @SuppressWarnings("unchecked")
    private AgentResponse parseToolResponse(String response) {
        try {
            Map<String, Object> jsonResponse = objectMapper.readValue(response, Map.class);
            Map<String, Object> result = (Map<String, Object>) jsonResponse.get("result");
            
            if (result != null) {
                List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
                if (content != null && !content.isEmpty()) {
                    Map<String, Object> firstContent = content.get(0);
                    String text = (String) firstContent.get("text");
                    return new AgentResponse(text != null ? text : "工具执行成功", true, new ArrayList<>());
                }
            }
            
            return new AgentResponse("工具执行成功", true, new ArrayList<>());
            
        } catch (Exception e) {
            System.err.println("⚠️  解析 MCP 响应失败: " + e.getMessage());
            return new AgentResponse(response, true, new ArrayList<>());
        }
    }
    
    @Override
    public void cleanup() {
        if (mcpProcess != null && mcpProcess.isAlive()) {
            mcpProcess.destroy();
            try {
                mcpProcess.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                mcpProcess.destroyForcibly();
            }
        }
    }
    
    /**
     * 查找 npx 命令路径
     */
    private String findNpxCommand() {
        // 尝试查找 npx
        String[] commands = {"npx", "npx.cmd"};
        for (String cmd : commands) {
            try {
                Process process = new ProcessBuilder("which", cmd).start();
                if (process.waitFor() == 0) {
                    return cmd;
                }
            } catch (Exception e) {
                // 继续尝试
            }
        }
        return "npx"; // 默认返回
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public List<MCPToolInfo> getTools() {
        return tools;
    }
}

