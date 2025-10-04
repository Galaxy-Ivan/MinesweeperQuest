package com.mcflurry.minesweeperquest.service;

import com.mcflurry.minesweeperquest.model.Task;
import com.mcflurry.minesweeperquest.repo.TaskRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

    public List<Task> getAll() {
        return repo.findAll();
    }

    public Task createTask(String publisher, String level, String details) {
        Task t = new Task(UUID.randomUUID().toString(), publisher, "", level, details);
        repo.add(t);
        broadcast();
        return t;
    }

    public boolean claimTask(String id, String user) {
        Optional<Task> opt = repo.findById(id);
        if (opt.isEmpty()) return false;
        Task t = opt.get();
        if (t.getAssignee() != null && !t.getAssignee().isEmpty()) return false; // 已被领取
        t.setAssignee(user);
        repo.update(t);
        broadcast();
        return true;
    }

    public boolean unclaimTask(String id, String user) {
        Optional<Task> opt = repo.findById(id);
        if (opt.isEmpty()) return false;
        Task t = opt.get();
        if (t.getAssignee() == null || !t.getAssignee().equals(user)) return false; // 只能放弃自己领取的任务
        t.setAssignee("");
        repo.update(t);
        broadcast();
        return true;
    }

    public boolean deleteTask(String id, String user) {
        Optional<Task> opt = repo.findById(id);
        if (opt.isEmpty()) return false;
        Task t = opt.get();
        if (!t.getPublisher().equals(user)) return false; // 只有发布者可以删除
        repo.deleteById(id);
        broadcast();
        return true;
    }

    private void broadcast() {
        List<Task> all = repo.findAll();
        messaging.convertAndSend("/topic/tasks", all);
    }
}
