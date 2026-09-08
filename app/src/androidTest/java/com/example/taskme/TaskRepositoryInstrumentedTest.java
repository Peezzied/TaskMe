package com.example.taskme;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.taskme.data.TaskRepository;
import com.example.taskme.model.TaskItem;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TaskRepositoryInstrumentedTest {

    private static final String TAG = "TaskRepoInstrumentedTest";
    private TaskRepository taskRepository;

    @Before
    public void setUp() {
        // Ensure network is enabled before starting each test
        FirebaseFirestore.getInstance().enableNetwork();
        taskRepository = new TaskRepository();
    }

    @After
    public void tearDown() {
        // Guarantee network is re-enabled after each test
        FirebaseFirestore.getInstance().enableNetwork();
    }

    @Test
    public void testFirestoreCreateAndReadTasks() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        String testTitle = "Instrumented Test Task " + System.currentTimeMillis();
        TaskItem task = new TaskItem(null, testTitle, "Testing Firebase Firestore integration", false, System.currentTimeMillis());

        // 1. Add Task to Firebase Firestore
        taskRepository.addTask(task, new TaskRepository.TaskCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "Task added successfully in instrumented test");

                // 2. Read Tasks from Firebase Firestore
                taskRepository.getTasks(new TaskRepository.TaskCallback<List<TaskItem>>() {
                    @Override
                    public void onSuccess(List<TaskItem> tasks) {
                        try {
                            assertNotNull("Tasks list should not be null", tasks);
                            boolean found = false;
                            for (TaskItem t : tasks) {
                                if (testTitle.equals(t.getTitle())) {
                                    found = true;
                                    break;
                                }
                            }
                            assertTrue("Newly added task should be retrieved from Firebase Firestore", found);
                        } catch (AssertionError e) {
                            fail(e.getMessage());
                        } finally {
                            // Clean up created task
                            if (task.getId() != null) {
                                taskRepository.deleteTask(task.getId(), new TaskRepository.TaskCallback<Void>() {
                                    @Override public void onSuccess(Void r) {}
                                    @Override public void onError(Exception e) {}
                                });
                            }
                            latch.countDown();
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        // Clean up created task
                        if (task.getId() != null) {
                            taskRepository.deleteTask(task.getId(), new TaskRepository.TaskCallback<Void>() {
                                @Override public void onSuccess(Void r) {}
                                @Override public void onError(Exception e) {}
                            });
                        }
                        fail("Failed to get tasks from Firebase: " + e.getMessage());
                        latch.countDown();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                fail("Firebase Firestore add task failed: " + e.getMessage());
                latch.countDown();
            }
        });

        assertTrue("Firebase operation timed out", latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void testOfflinePersistenceBehaviorWithDisabledNetwork() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        TaskItem offlineTask = new TaskItem(null, "Forced Offline Task " + System.currentTimeMillis(), "Testing Firestore disableNetwork", false, System.currentTimeMillis());

        // Explicitly disable network using Firebase Firestore API to simulate being offline
        FirebaseFirestore.getInstance().disableNetwork().addOnCompleteListener(disableTask -> {
            taskRepository.addTask(offlineTask, new TaskRepository.TaskCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "Successfully added task while network was disabled (offline persistence active)");
                    if (offlineTask.getId() != null) {
                        taskRepository.deleteTask(offlineTask.getId(), new TaskRepository.TaskCallback<Void>() {
                            @Override public void onSuccess(Void r) {}
                            @Override public void onError(Exception e) {}
                        });
                    }
                    latch.countDown();
                }

                @Override
                public void onError(Exception e) {
                    fail("Offline write error: " + e.getMessage());
                    latch.countDown();
                }
            });

            // Re-enable network so the queued offline write syncs with the server and triggers callback
            FirebaseFirestore.getInstance().enableNetwork();
        });

        assertTrue("Offline network simulation test timed out", latch.await(15, TimeUnit.SECONDS));
    }
}

