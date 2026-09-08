package com.example.taskme.adapter;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.taskme.R;
import com.example.taskme.model.TaskItem;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final List<TaskItem> taskList;
    private final OnTaskActionListener actionListener;

    public interface OnTaskActionListener {
        void onTaskCheckedChange(TaskItem task, boolean isChecked);
        void onTaskDelete(TaskItem task);
    }

    public TaskAdapter(List<TaskItem> taskList, OnTaskActionListener actionListener) {
        this.taskList = taskList;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskItem task = taskList.get(position);
        holder.titleTextView.setText(task.getTitle());
        
        if (task.getDescription() != null && !task.getDescription().isEmpty()) {
            holder.descriptionTextView.setText(task.getDescription());
            holder.descriptionTextView.setVisibility(View.VISIBLE);
        } else {
            holder.descriptionTextView.setVisibility(View.GONE);
        }

        // Simple item entrance animation
        holder.itemView.setAlpha(0f);
        holder.itemView.setTranslationY(30f);
        holder.itemView.animate().alpha(1f).translationY(0f).setDuration(250).setStartDelay(position * 40L).start();

        // Prevent unwanted callback triggers during binding
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(task.isCompleted());
        updateStrikeThrough(holder.titleTextView, task.isCompleted());

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.setCompleted(isChecked);
            updateStrikeThrough(holder.titleTextView, isChecked);
            if (actionListener != null) {
                actionListener.onTaskCheckedChange(task, isChecked);
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onTaskDelete(task);
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    private void updateStrikeThrough(TextView textView, boolean isCompleted) {
        if (isCompleted) {
            textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            textView.setAlpha(0.6f);
        } else {
            textView.setPaintFlags(textView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            textView.setAlpha(1.0f);
        }
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView titleTextView;
        TextView descriptionTextView;
        ImageButton deleteButton;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkboxTask);
            titleTextView = itemView.findViewById(R.id.textTaskTitle);
            descriptionTextView = itemView.findViewById(R.id.textTaskDescription);
            deleteButton = itemView.findViewById(R.id.btnDeleteTask);
        }
    }
}
