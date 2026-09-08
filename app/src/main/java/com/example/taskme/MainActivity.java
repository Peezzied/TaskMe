package com.example.taskme;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.taskme.adapter.TaskAdapter;
import com.example.taskme.data.TaskRepository;
import com.example.taskme.model.TaskItem;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TaskAdapter.OnTaskActionListener {

    private static final String TAG = "MainActivity";

    private FloatingActionButton fabAddTask;
    private MaterialToolbar topAppBar;
    private RecyclerView recyclerViewTasks;
    private ProgressBar progressBar;
    private TextView textEmptyState;

    private TaskRepository taskRepository;
    private TaskAdapter taskAdapter;
    private final List<TaskItem> taskList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        try {
            initViews();
            initRepository();
            setupRecyclerView();
            setupListeners();
            animateFab();
            checkInternetAndNotify();
            loadTasks();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            showError(getString(R.string.error_loading_tasks));
        }
    }

    private void initViews() {
        fabAddTask = findViewById(R.id.fabAddTask);
        topAppBar = findViewById(R.id.topAppBar);
        recyclerViewTasks = findViewById(R.id.recyclerViewTasks);
        progressBar = findViewById(R.id.progressBar);
        textEmptyState = findViewById(R.id.textEmptyState);
    }

    private void initRepository() {
        try {
            taskRepository = new TaskRepository();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize TaskRepository", e);
            taskRepository = null;
        }
    }

    private void setupRecyclerView() {
        taskAdapter = new TaskAdapter(taskList, this);
        recyclerViewTasks.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewTasks.setAdapter(taskAdapter);
    }

    private void setupListeners() {
        fabAddTask.setOnClickListener(v -> {
            try {
                showAddTaskModal();
            } catch (Exception e) {
                Log.e(TAG, "Error opening add task modal", e);
                showError("Could not open task form");
            }
        });

        topAppBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_sync) {
                try {
                    checkInternetAndNotify();
                    loadTasks();
                    showSnackbar(getString(R.string.tasks_synced));
                } catch (Exception e) {
                    Log.e(TAG, "Error syncing tasks", e);
                    showError("Failed to sync tasks");
                }
                return true;
            }
            return false;
        });
    }

    private void animateFab() {
        fabAddTask.setScaleX(0f);
        fabAddTask.setScaleY(0f);
        fabAddTask.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(350)
                .setStartDelay(200)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }

    private void checkInternetAndNotify() {
        if (!isNetworkAvailable(this)) {
            showSnackbar(getString(R.string.offline_mode_active));
        }
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            );
        }
        return false;
    }

    private void showAddTaskModal() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_task, null);
        bottomSheetDialog.setContentView(dialogView);

        TextInputLayout dialogInputLayoutTitle = dialogView.findViewById(R.id.dialogInputLayoutTitle);
        TextInputEditText dialogInputEditTextTitle = dialogView.findViewById(R.id.dialogInputEditTextTitle);
        TextInputEditText dialogInputEditTextDescription = dialogView.findViewById(R.id.dialogInputEditTextDescription);
        MaterialButton dialogButtonAddTask = dialogView.findViewById(R.id.dialogButtonAddTask);

        dialogButtonAddTask.setOnClickListener(v -> {
            String title = dialogInputEditTextTitle.getText() != null ? dialogInputEditTextTitle.getText().toString().trim() : "";
            String description = dialogInputEditTextDescription.getText() != null ? dialogInputEditTextDescription.getText().toString().trim() : "";

            if (TextUtils.isEmpty(title)) {
                dialogInputLayoutTitle.setError(getString(R.string.error_empty_title));
                return;
            } else {
                dialogInputLayoutTitle.setError(null);
            }

            if (taskRepository == null) {
                showError("TaskRepository not initialized");
                return;
            }

            TaskItem newTask = new TaskItem(null, title, description, false, System.currentTimeMillis());

            // Dismiss modal form immediately for seamless responsive UX
            bottomSheetDialog.dismiss();

            boolean isOffline = !isNetworkAvailable(MainActivity.this);

            // Optimistically update list and UI immediately
            taskList.add(0, newTask);
            taskAdapter.notifyItemInserted(0);
            recyclerViewTasks.scrollToPosition(0);
            updateEmptyState();

            if (isOffline) {
                showSnackbar(getString(R.string.task_saved_offline));
            } else {
                showSnackbar(getString(R.string.task_added_success));
            }

            // Asynchronously save to repository (writes to local cache + syncs when connected)
            taskRepository.addTask(newTask, new TaskRepository.TaskCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "Task saved successfully: " + newTask.getTitle());
                }

                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "Failed to persist task", e);
                        showError(getString(R.string.error_adding_task) + ": " + e.getMessage());
                        taskList.remove(newTask);
                        taskAdapter.notifyDataSetChanged();
                        updateEmptyState();
                    });
                }
            });
        });

        bottomSheetDialog.show();
    }

    private void loadTasks() {
        if (taskRepository == null) {
            showError("TaskRepository not initialized");
            updateEmptyState();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        boolean isOffline = !isNetworkAvailable(this);

        taskRepository.getTasks(isOffline, new TaskRepository.TaskCallback<List<TaskItem>>() {
            @Override
            public void onSuccess(List<TaskItem> result) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    taskList.clear();
                    if (result != null) {
                        taskList.addAll(result);
                    }
                    taskAdapter.notifyDataSetChanged();
                    updateEmptyState();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Failed to load tasks", e);
                    showError(getString(R.string.error_loading_tasks) + ": " + e.getMessage());
                    updateEmptyState();
                });
            }
        });
    }

    @Override
    public void onTaskCheckedChange(TaskItem task, boolean isChecked) {
        try {
            if (taskRepository != null) {
                taskRepository.updateTaskCompletion(task.getId(), isChecked, new TaskRepository.TaskCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.d(TAG, "Task completion updated: " + task.getId());
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Failed to update task completion", e);
                        showError("Failed to update task status");
                        loadTasks(); // reload to revert view state on failure
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in onTaskCheckedChange", e);
            showError("Error updating task");
        }
    }

    @Override
    public void onTaskDelete(TaskItem task) {
        try {
            if (taskRepository != null) {
                int position = taskList.indexOf(task);
                if (position != -1) {
                    taskList.remove(position);
                    taskAdapter.notifyItemRemoved(position);
                    updateEmptyState();
                }
                showSnackbar(getString(R.string.task_deleted_success));

                taskRepository.deleteTask(task.getId(), new TaskRepository.TaskCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Log.d(TAG, "Task deleted successfully: " + task.getId());
                    }

                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> {
                            Log.e(TAG, "Failed to delete task", e);
                            showError("Failed to delete task");
                            loadTasks();
                        });
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in onTaskDelete", e);
            showError("Error deleting task");
        }
    }

    private void updateEmptyState() {
        if (taskList.isEmpty()) {
            textEmptyState.setVisibility(View.VISIBLE);
            recyclerViewTasks.setVisibility(View.GONE);
        } else {
            textEmptyState.setVisibility(View.GONE);
            recyclerViewTasks.setVisibility(View.VISIBLE);
        }
    }

    private void showSnackbar(String message) {
        try {
            Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing snackbar", e);
        }
    }

    private void showError(String message) {
        try {
            Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(getResources().getColor(android.R.color.holo_red_dark))
                    .setTextColor(getResources().getColor(android.R.color.white))
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing error message", e);
        }
    }
}
