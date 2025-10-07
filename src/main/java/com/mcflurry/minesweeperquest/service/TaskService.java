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
    private static final String SUPER_USER = "McFlurry!";

    public TaskService(TaskRepository repo, SimpMessagingTemplate messaging) {
        this.repo = repo;
        this.messaging = messaging;
    }

    // 每天早上 8 点清空所有任务
    @Scheduled(cron = "55 59 7 * * *")
    public void clearTasksEveryMorning() {
        try {
            Path src = Paths.get("data/tasks.json");
            LocalDate yesterday = LocalDate.now().minusDays(1);
            String dateStr = yesterday.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            Path backupDir = Paths.get("data/backups");
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }
            Path backup = backupDir.resolve(dateStr + "_tasks.json");
            if (Files.exists(src)) {
                Files.copy(src, backup, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("已备份任务文件：" + backup);
            }
        } catch (IOException e) {
            System.err.println("备份任务文件时出错：" + e.getMessage());
        }
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
        if (!"未接".equals(t.getStatus()) && !"预约中".equals(t.getStatus())) return false;
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

    public boolean reserveTask(String id, String user) {
        Optional<Task> opt = repo.findById(id);
        if (opt.isEmpty()) return false;
        Task t = opt.get();
        if (!"未接".equals(t.getStatus())) return false; // 只能预约未接任务
        t.setAssignee(user);
        t.setStatus("预约中");
        repo.update(t);
        broadcast();
        return true;
    }

    public boolean unreserveTask(String id, String user) {
        Optional<Task> opt = repo.findById(id);
        if (opt.isEmpty()) return false;
        Task t = opt.get();
        if (!"预约中".equals(t.getStatus()) || !user.equals(t.getAssignee())) return false;
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
        if ((!user.equals(t.getPublisher())) && (!user.equals(t.getAssignee()))) return false;
        if ((!"进行中".equals(t.getStatus())) && (!"预约中".equals(t.getStatus()))) return false;
        t.setStatus("已发送");
        repo.update(t);
        broadcast();
        return true;
    }

    public boolean deleteTask(String id, String user) {
        Optional<Task> opt = repo.findById(id);
        if (opt.isEmpty()) return false;
        Task t = opt.get();
        boolean isSuperUser = SUPER_USER.equals(user);
        if (!isSuperUser && !t.getPublisher().equals(user)) return false;
        if ("已发送".equals(t.getStatus())) return false;
        repo.deleteById(id);
        broadcast();
        return true;
    }

    private void broadcast() {
        List<Task> all = repo.findAll();
        messaging.convertAndSend("/topic/tasks", all);
    }
}
