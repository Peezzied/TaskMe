package com.example.taskme.data;

import android.util.Log;

import com.example.taskme.model.TaskItem;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.PersistentCacheSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskRepository {
    private static final String TAG = "TaskRepository";
    private FirebaseFirestore db;

    public interface TaskCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public TaskRepository() {
        try {
            db = FirebaseFirestore.getInstance();
            try {
                FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                        .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                        .build();
                db.setFirestoreSettings(settings);
            } catch (Exception settingsException) {
                Log.w(TAG, "Firestore settings already configured or notice: " + settingsException.getMessage());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize FirebaseFirestore", e);
            db = null;
        }
    }

    public void addTask(TaskItem task, TaskCallback<Void> callback) {
        try {
            if (db == null) {
                Exception ex = new IllegalStateException("Firebase Firestore is not initialized.");
                Log.e(TAG, "addTask failed: db is null", ex);
                callback.onError(ex);
                return;
            }

            if (task.getId() == null || task.getId().isEmpty()) {
                String docId = db.collection(TaskContract.COLLECTION_TASKS).document().getId();
                task.setId(docId);
            }

            Map<String, Object> taskMap = new HashMap<>();
            taskMap.put(TaskContract.TaskEntry.FIELD_ID, task.getId());
            taskMap.put(TaskContract.TaskEntry.FIELD_TITLE, task.getTitle());
            taskMap.put(TaskContract.TaskEntry.FIELD_DESCRIPTION, task.getDescription());
            taskMap.put(TaskContract.TaskEntry.FIELD_IS_COMPLETED, task.isCompleted());
            taskMap.put(TaskContract.TaskEntry.FIELD_TIMESTAMP, task.getTimestamp());

            db.collection(TaskContract.COLLECTION_TASKS)
                    .document(task.getId())
                    .set(taskMap)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Task added successfully: " + task.getTitle());
                        callback.onSuccess(null);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error adding task to Firestore", e);
                        callback.onError(e);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception caught in addTask", e);
            callback.onError(e);
        }
    }

    public void getTasks(TaskCallback<List<TaskItem>> callback) {
        getTasks(false, callback);
    }

    public void getTasks(boolean isOffline, TaskCallback<List<TaskItem>> callback) {
        try {
            if (db == null) {
                Exception ex = new IllegalStateException("Firebase Firestore is not initialized.");
                Log.e(TAG, "getTasks failed: db is null", ex);
                callback.onError(ex);
                return;
            }

            Source source = isOffline ? Source.CACHE : Source.DEFAULT;

            db.collection(TaskContract.COLLECTION_TASKS)
                    .orderBy(TaskContract.TaskEntry.FIELD_TIMESTAMP, Query.Direction.DESCENDING)
                    .get(source)
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<TaskItem> tasks = new ArrayList<>();
                        try {
                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                TaskItem task = doc.toObject(TaskItem.class);
                                tasks.add(task);
                            }
                            callback.onSuccess(tasks);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing task documents", e);
                            callback.onError(e);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!isOffline) {
                            Log.w(TAG, "Network fetch failed, attempting to read tasks from local cache", e);
                            db.collection(TaskContract.COLLECTION_TASKS)
                                    .orderBy(TaskContract.TaskEntry.FIELD_TIMESTAMP, Query.Direction.DESCENDING)
                                    .get(Source.CACHE)
                                    .addOnSuccessListener(cacheSnapshots -> {
                                        List<TaskItem> cachedTasks = new ArrayList<>();
                                        try {
                                            for (QueryDocumentSnapshot doc : cacheSnapshots) {
                                                TaskItem task = doc.toObject(TaskItem.class);
                                                cachedTasks.add(task);
                                            }
                                            callback.onSuccess(cachedTasks);
                                        } catch (Exception parseException) {
                                            Log.e(TAG, "Error parsing cached task documents", parseException);
                                            callback.onError(parseException);
                                        }
                                    })
                                    .addOnFailureListener(cacheErr -> {
                                        Log.e(TAG, "Error fetching tasks from Firestore cache", cacheErr);
                                        callback.onError(e);
                                    });
                        } else {
                            Log.e(TAG, "Error fetching tasks from Firestore cache", e);
                            callback.onError(e);
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception caught in getTasks", e);
            callback.onError(e);
        }
    }

    public void updateTaskCompletion(String taskId, boolean isCompleted, TaskCallback<Void> callback) {
        try {
            if (db == null) {
                Exception ex = new IllegalStateException("Firebase Firestore is not initialized.");
                Log.e(TAG, "updateTaskCompletion failed: db is null", ex);
                callback.onError(ex);
                return;
            }

            db.collection(TaskContract.COLLECTION_TASKS)
                    .document(taskId)
                    .update(TaskContract.TaskEntry.FIELD_IS_COMPLETED, isCompleted)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Task completion updated successfully: " + taskId);
                        callback.onSuccess(null);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error updating task completion", e);
                        callback.onError(e);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception caught in updateTaskCompletion", e);
            callback.onError(e);
        }
    }

    public void deleteTask(String taskId, TaskCallback<Void> callback) {
        try {
            if (db == null) {
                Exception ex = new IllegalStateException("Firebase Firestore is not initialized.");
                Log.e(TAG, "deleteTask failed: db is null", ex);
                callback.onError(ex);
                return;
            }

            db.collection(TaskContract.COLLECTION_TASKS)
                    .document(taskId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Task deleted successfully: " + taskId);
                        callback.onSuccess(null);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error deleting task", e);
                        callback.onError(e);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception caught in deleteTask", e);
            callback.onError(e);
        }
    }
}
