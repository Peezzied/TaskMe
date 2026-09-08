package com.example.taskme.data;

public final class TaskContract {
    private TaskContract() {}

    public static final String COLLECTION_TASKS = "tasks";

    public static final class TaskEntry {
        public static final String FIELD_ID = "id";
        public static final String FIELD_TITLE = "title";
        public static final String FIELD_DESCRIPTION = "description";
        public static final String FIELD_IS_COMPLETED = "isCompleted";
        public static final String FIELD_TIMESTAMP = "timestamp";
    }
}
