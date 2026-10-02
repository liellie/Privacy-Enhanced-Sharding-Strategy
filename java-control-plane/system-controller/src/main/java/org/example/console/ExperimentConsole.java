package org.example.console;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Slf4j
@Component
public class ExperimentConsole implements ApplicationRunner {

    private static final String PROMPT = "shardchain> ";

    @Autowired
    private ExperimentConsoleService consoleService;

    private volatile boolean running = true;

    @Override
    public void run(ApplicationArguments args) {
        Thread thread = new Thread(this::repl, "experiment-console");
        thread.setDaemon(true);
        thread.start();
         
    }

    private void repl() {
        banner();
        help();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while (running) {
                System.out.print(PROMPT);
                System.out.flush();
                line = reader.readLine();
                if (line == null) {
                    break;
                }
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                String[] parts = trimmed.split("\\s+");
                String cmd = parts[0].toLowerCase();
                String[] params = Arrays.copyOfRange(parts, 1, parts.length);
                if (cmd.equals("exit") || cmd.equals("quit")) {
                    running = false;
                    println("控制台已退出，应用继续运行（停止所有节点请执行 shutdown，或直接结束进程）");
                    break;
                }
                if (cmd.equals("clear")) {
                    clearScreen();
                    continue;
                }
                dispatch(cmd, params);
            }
        } catch (IOException e) {
            log.warn("实验控制台输入流不可用（无控制台环境），已退出交互模式：{}", e.getMessage());
        }
    }

    private void dispatch(String cmd, String[] params) {
        try {
            switch (cmd) {
                 
                case "help" -> help();
                case "init" -> consoleService.init();
                case "start" -> consoleService.start();
                case "stop" -> consoleService.stop();
                case "finish" -> consoleService.finish();
                case "shutdown" -> consoleService.shutdown();
                 
                case "status" -> consoleService.status();
                case "epoch" -> consoleService.epoch();
                case "stats" -> consoleService.stats();
                case "results" -> consoleService.results(params);
                case "requests" -> consoleService.requests(params);
                case "receipts" -> consoleService.receipts(params);
                case "tasks" -> consoleService.tasks(params);
                default -> println("未知命令: " + cmd + "，输入 help 查看命令列表");
            }
        } catch (IllegalArgumentException e) {
            println("[参数错误] " + e.getMessage());
        } catch (Exception e) {
            println("[执行失败] " + e.getMessage());
        }
    }

    private void banner() {
        println();
        println("============================================================");
        println("        分片链（ShardChain）实验控制台");
        println("        输入 help 查看命令列表，exit 退出控制台");
        println("============================================================");
        println();
    }

    private void help() {
        println("======== 命令列表 ========");
        println("  --- 实验生命周期（先 init 后 start） ---");
        println("  init                        初始化所有节点（协调者 → 分片 → Agent）");
        println("  start                       开始运行（下发 start 激活各节点定时任务，协调者置 RUNNING）");
        println("  stop                        暂停运行（下发 stop 关闭各节点定时任务，协调者置 PAUSED）");
        println("  finish                      结束实验并生成报告（协调者置 FINISHED + Markdown 报告）");
        println("  shutdown                    关闭所有节点进程（协调者 → Agent → 分片）");
        println("  --- 实验数据与结果 ---");
        println("  status                      查看全局状态（协调者/分片/Agent/任务）");
        println("  epoch                       查看当前 Epoch 与协调者状态");
        println("  stats                       查看实验统计（回执/成功率）");
        println("  results [limit]             查看实验结果（回执统计+分片聚合+任务+明细）");
        println("  requests [limit]            查看最近交易请求");
        println("  receipts [limit]            查看最近交易回执");
        println("  tasks [状态]                查看任务列表（可选 PENDING/PROCESSING/COMPLETED/FAILED）");
        println("  --- 其他 ---");
        println("  clear                       清屏");
        println("  exit | quit                 退出控制台（应用继续运行）");
        println("===========================");
    }

    private void clearScreen() {
        System.out.print("\033[2J\033[H");
        System.out.flush();
    }

    private static void println(Object o) {
        System.out.println(o);
    }

    private static void println() {
        System.out.println();
    }
}