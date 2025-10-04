package com.mcflurry.minesweeperquest.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Task {
    private String id;
    private String publisher;
    private String assignee; // "" 表示无人领取
    private String level;
    private String details;

    public Task() {}

    public Task(String id, String publisher, String assignee, String level, String details) {
        this.id = id;
        this.publisher = publisher;
        this.assignee = assignee;
        this.level = level;
        this.details = details;
    }

    // getters and setters
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
}
