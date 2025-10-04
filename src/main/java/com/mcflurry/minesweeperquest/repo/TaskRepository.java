package com.mcflurry.minesweeperquest.repo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcflurry.minesweeperquest.model.Task;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Component
public class TaskRepository {
    private final Path file = Paths.get("data", "tasks.json");
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @PostConstruct
    private void init() {
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                List<Task> sample = sampleTasks();
                writeTasks(sample);
            }
        } catch (IOException e) {
            throw new RuntimeException("初始化 tasks.json 失败", e);
        }
    }

    private List<Task> sampleTasks() {
        List<Task> list = new ArrayList<>();
        list.add(new Task(UUID.randomUUID().toString(), "onion", "", "L5", "完成一局高级"));
        list.add(new Task(UUID.randomUUID().toString(), "McFlurry", "", "L8", "获得 50000 经验"));
        return list;
    }

    public List<Task> findAll() {
        lock.readLock().lock();
        try {
            if (!Files.exists(file)) return Collections.emptyList();
            byte[] bytes = Files.readAllBytes(file);
            if (bytes.length == 0) return Collections.emptyList();
            return mapper.readValue(bytes, new TypeReference<List<Task>>() {});
        } catch (IOException e) {
            throw new RuntimeException("读取 tasks.json 失败", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Optional<Task> findById(String id) {
        return findAll().stream().filter(t -> t.getId().equals(id)).findFirst();
    }

    public void writeTasks(List<Task> tasks) {
        lock.writeLock().lock();
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), tasks);
        } catch (IOException e) {
            throw new RuntimeException("写入 tasks.json 失败", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // 辅助方法：添加、更新、删除
    public Task add(Task task) {
        List<Task> all = new ArrayList<>(findAll());
        all.add(task);
        writeTasks(all);
        return task;
    }

    public void update(Task task) {
        List<Task> all = findAll().stream()
                .map(t -> t.getId().equals(task.getId()) ? task : t)
                .collect(Collectors.toList());
        writeTasks(all);
    }

    public void deleteById(String id) {
        List<Task> all = findAll().stream()
                .filter(t -> !t.getId().equals(id))
                .collect(Collectors.toList());
        writeTasks(all);
    }

    // ✅ 新增：清空所有任务
    public void clearAll() {
        writeTasks(new ArrayList<>());
    }
}
