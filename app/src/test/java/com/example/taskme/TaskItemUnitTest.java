package com.example.taskme;

import com.example.taskme.data.TaskContract;
import com.example.taskme.model.TaskItem;

import org.junit.Test;
import static org.junit.Assert.*;

public class TaskItemUnitTest {

    @Test
    public void testTaskItemCreationAndGettersSetters() {
        TaskItem task = new TaskItem("123", "Test Title", "Test Desc", false, 1000L);
        assertEquals("123", task.getId());
        assertEquals("Test Title", task.getTitle());
        assertEquals("Test Desc", task.getDescription());
        assertFalse(task.isCompleted());
        assertEquals(1000L, task.getTimestamp());

        task.setCompleted(true);
        assertTrue(task.isCompleted());
        task.setTitle("Updated Title");
        assertEquals("Updated Title", task.getTitle());
    }

    @Test
    public void testTaskContractConstants() {
        assertEquals("tasks", TaskContract.COLLECTION_TASKS);
        assertEquals("id", TaskContract.TaskEntry.FIELD_ID);
        assertEquals("title", TaskContract.TaskEntry.FIELD_TITLE);
        assertEquals("description", TaskContract.TaskEntry.FIELD_DESCRIPTION);
        assertEquals("isCompleted", TaskContract.TaskEntry.FIELD_IS_COMPLETED);
        assertEquals("timestamp", TaskContract.TaskEntry.FIELD_TIMESTAMP);
    }
}
