package com.mcflurry.minesweeperquest.service;

import com.mcflurry.minesweeperquest.model.Task;
import com.mcflurry.minesweeperquest.repo.TaskRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class TaskService {
    private final TaskRepository repo;
    private final SimpMessagingTemplate messaging;
    // 在前端页面中，有对这个字符串的二次校验，若需修改，两个地方都要改，不然不会渲染出来按钮
    private static final String SUPER_USER = "McFlurry!";

    public TaskService(TaskRepository repo, SimpMessagingTemplate messaging) {
        this.repo = repo;
        this.messaging = messaging;
    }

    // 每天早上 8 点清空所有任务
    @Scheduled(cron = "55 59 7 * * *")
    public void clearTasksEveryMorning() {
        try {
            // 源文件路径
            Path src = Paths.get("data/tasks.json");

            // 前一天日期
            LocalDate yesterday = LocalDate.now().minusDays(1);
            String dateStr = yesterday.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            // 备份目录路径
            Path backupDir = Paths.get("data/backups");
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }

            // 备份文件路径
            Path backup = backupDir.resolve(dateStr + "_tasks.json");

            // 如果原文件存在，则备份
            if (Files.exists(src)) {
                Files.copy(src, backup, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("已备份任务文件：" + backup);
            } else {
                System.out.println("未找到 data/tasks.json，跳过备份。");
            }
        } catch (IOException e) {
            System.err.println("备份任务文件时出错：" + e.getMessage());
        }

        // 清空任务数据
        repo.clearAll();
        broadcast();
        System.out.println("已清空所有任务 (每天 8 点自动执行)");
    }


    public List<Task> getAll() {
        return repo.findAll();
    }

    public Task createTask(String publisher, String level, String details) {
        Task t = new Task(UUID.randomUUID().toString(), publisher, "", level, details);
        t.setStatus("未接");
        repo.add(t);
        broadcast();
        return t;
    }

    public boolean claimTask(String id, String user) {
        Optional<Task> opt = repo.findById(id);
        if (opt.isEmpty()) return false;
        Task t = opt.get();
        if (!"未接".equals(t.getStatus())) return false;
        t.setAssignee(user);
        t.setStatus("进行中");
        repo.update(t);
        broadcast();
        return true;
    }

    public boolean unclaimTask(String id, String user) {
        Optional<Task> opt = repo.findById(id);
        if (opt.isEmpty()) return false;
        Task t = opt.get();
        if (!user.equals(t.getAssignee())) return false;
        if (!"进行中".equals(t.getStatus())) return false;
        t.setAssignee("");
        t.setStatus("未接");
        repo.update(t);
        broadcast();
        return true;
    }

    public boolean completeTask(String id, String user) {
        Optional<Task> opt = repo.findById(id);
        if (opt.isEmpty()) return false;
        Task t = opt.get();
        if (!user.equals(t.getPublisher())) return false;
        if (!"进行中".equals(t.getStatus())) return false;
        t.setStatus("已完成");
        repo.update(t);
        broadcast();
        return true;
    }

    public boolean deleteTask(String id, String user) {
        Optional<Task> opt = repo.findById(id);
        if (opt.isEmpty()) return false;
        Task t = opt.get();
        boolean isSuperUser = "McFlurry!".equals(user); // ✅ 特殊账号
        if (!isSuperUser && !t.getPublisher().equals(user)) return false;
        // 允许撤下未完成的任务（只要还没被标记为已完成）
        if ("已完成".equals(t.getStatus())) return false;
        repo.deleteById(id);
        broadcast();
        return true;

    }

    private void broadcast() {
        List<Task> all = repo.findAll();
        messaging.convertAndSend("/topic/tasks", all);
    }
}
