package com.mcflurry.minesweeperquest.service;

import com.mcflurry.minesweeperquest.model.Task;
import com.mcflurry.minesweeperquest.repo.TaskRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TaskService {
    private final TaskRepository repo;
    private final SimpMessagingTemplate messaging;

    public TaskService(TaskRepository repo, SimpMessagingTemplate messaging) {
        this.repo = repo;
        this.messaging = messaging;
    }

    // 每天早上 8 点清空所有任务
    @Scheduled(cron = "0 0 8 * * *")
    public void clearTasksEveryMorning() {
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
        if (!t.getPublisher().equals(user)) return false;
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
