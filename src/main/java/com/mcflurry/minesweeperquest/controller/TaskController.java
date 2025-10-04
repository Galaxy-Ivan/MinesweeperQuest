package com.mcflurry.minesweeperquest.controller;

import com.mcflurry.minesweeperquest.model.Task;
import com.mcflurry.minesweeperquest.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping
    public List<Task> list() {
        return service.getAll();
    }

    // 创建任务（publisher 从请求中带入，不需要登录）
    @PostMapping
    public ResponseEntity<Task> create(@RequestBody Map<String, String> body) {
        String publisher = body.get("publisher");
        String level = body.get("level");
        String details = body.get("details");
        if (publisher == null || publisher.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Task t = service.createTask(publisher.trim(), level == null ? "" : level, details == null ? "" : details);
        return ResponseEntity.ok(t);
    }

    // 领取
    @PostMapping("/{id}/claim")
    public ResponseEntity<?> claim(@PathVariable String id, @RequestBody Map<String, String> body) {
        String user = body.get("user");
        if (user == null || user.trim().isEmpty()) return ResponseEntity.badRequest().body("missing user");
        boolean ok = service.claimTask(id, user.trim());
        return ok ? ResponseEntity.ok().build() : ResponseEntity.status(409).body("cannot claim");
    }

    // 放弃
    @PostMapping("/{id}/unclaim")
    public ResponseEntity<?> unclaim(@PathVariable String id, @RequestBody Map<String, String> body) {
        String user = body.get("user");
        if (user == null || user.trim().isEmpty()) return ResponseEntity.badRequest().body("missing user");
        boolean ok = service.unclaimTask(id, user.trim());
        return ok ? ResponseEntity.ok().build() : ResponseEntity.status(409).body("cannot unclaim");
    }

    // 删除（只有发布者可删除）
    @PostMapping("/{id}/delete")
    public ResponseEntity<?> delete(@PathVariable String id, @RequestBody Map<String, String> body) {
        String user = body.get("user");
        if (user == null || user.trim().isEmpty()) return ResponseEntity.badRequest().body("missing user");
        boolean ok = service.deleteTask(id, user.trim());
        return ok ? ResponseEntity.ok().build() : ResponseEntity.status(403).body("forbidden");
    }
}
