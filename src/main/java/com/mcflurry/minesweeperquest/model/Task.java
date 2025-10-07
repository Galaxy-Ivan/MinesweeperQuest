package com.mcflurry.minesweeperquest.model;

public class Task {
    private String id;
    private String publisher;
    private String assignee;
    private String level;
    private String details;
    private String status; // "未接"、"进行中"、"已发送"

    public Task() {}

    public Task(String id, String publisher, String assignee, String level, String details) {
        this.id = id;
        this.publisher = publisher;
        this.assignee = assignee;
        this.level = level;
        this.details = details;
        this.status = "未接";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", publisher='" + publisher + '\'' +
                ", assignee='" + assignee + '\'' +
                ", level='" + level + '\'' +
                ", details='" + details + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
