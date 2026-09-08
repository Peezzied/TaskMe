package com.example.taskme.model;

import com.google.firebase.firestore.PropertyName;

import java.io.Serializable;

public class TaskItem implements Serializable {
    private String id;
    private String title;
    private String description;
    private boolean isCompleted;
    private long timestamp;

    public TaskItem() {
        // Public no-arg constructor required for Firestore
    }

    public TaskItem(String id, String title, String description, boolean isCompleted, long timestamp) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.isCompleted = isCompleted;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @PropertyName("isCompleted")
    public boolean isCompleted() {
        return isCompleted;
    }

    @PropertyName("isCompleted")
    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
