package com.example.taskme;

import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.anyOf;

@RunWith(AndroidJUnit4.class)
public class MainActivityInstrumentedTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() {
        // Ensure network is enabled before every test
        FirebaseFirestore.getInstance().enableNetwork();
    }

    @After
    public void tearDown() {
        // Ensure network is restored after every test
        FirebaseFirestore.getInstance().enableNetwork();
    }

    @Test
    public void testUiComponentsDisplayed() {
        onView(withId(R.id.fabAddTask)).check(matches(isDisplayed()));
        onView(withId(R.id.topAppBar)).check(matches(isDisplayed()));
        waitForAnyView(5000, R.id.recyclerViewTasks, R.id.textEmptyState);
    }

    @Test
    public void testUserInputValidation() {
        // Open modal form via FAB
        onView(withId(R.id.fabAddTask)).perform(click());
        // Try clicking add task with empty title
        onView(withId(R.id.dialogButtonAddTask)).perform(click());
        // Verify modal form remains open for user correction
        onView(withId(R.id.dialogInputEditTextTitle)).check(matches(isDisplayed()));
    }

    @Test
    public void testAddTaskAndDisplay() {
        // Open modal form via FAB
        onView(withId(R.id.fabAddTask)).perform(click());
        // Type title and description
        onView(withId(R.id.dialogInputEditTextTitle))
                .perform(typeText("Complete assignment"), ViewActions.closeSoftKeyboard());
        onView(withId(R.id.dialogInputEditTextDescription))
                .perform(typeText("Submit CIT306 project work"), ViewActions.closeSoftKeyboard());
        // Click add task button inside modal
        onView(withId(R.id.dialogButtonAddTask)).perform(click());
        // Wait for async Firestore addition, dialog dismissal, and RecyclerView display
        waitForView(R.id.recyclerViewTasks, 5000);
    }

    @Test
    public void testSyncButton() {
        // Click sync action item in top app bar
        onView(withId(R.id.action_sync)).perform(click());
        waitForAnyView(5000, R.id.recyclerViewTasks, R.id.textEmptyState);
    }

    @Test
    public void testOfflineFeedbackHandling() {
        // Simulate no internet by disabling Firestore network
        FirebaseFirestore.getInstance().disableNetwork();

        // Perform sync action which triggers offline notification/handling
        onView(withId(R.id.action_sync)).perform(click());
        waitForAnyView(5000, R.id.recyclerViewTasks, R.id.textEmptyState);

        // Restore network
        FirebaseFirestore.getInstance().enableNetwork();
    }

    @Test
    public void testAddTaskWhileOffline() {
        // Simulate offline mode by disabling network
        FirebaseFirestore.getInstance().disableNetwork();

        // Open modal form via FAB
        onView(withId(R.id.fabAddTask)).perform(click());
        // Type title and description
        onView(withId(R.id.dialogInputEditTextTitle))
                .perform(typeText("Offline Task"), ViewActions.closeSoftKeyboard());
        onView(withId(R.id.dialogInputEditTextDescription))
                .perform(typeText("Created while offline"), ViewActions.closeSoftKeyboard());
        // Click add task button inside modal
        onView(withId(R.id.dialogButtonAddTask)).perform(click());

        // Wait for list update
        waitForAnyView(5000, R.id.recyclerViewTasks, R.id.textEmptyState);

        // Restore network
        FirebaseFirestore.getInstance().enableNetwork();
    }

    /**
     * Helper to poll until a specific view is displayed in the active window hierarchy.
     * Useful when waiting for async Firestore callbacks and BottomSheetDialog dismissal.
     */
    private void waitForView(int viewId, long timeoutMillis) {
        long endTime = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < endTime) {
            try {
                onView(withId(viewId)).check(matches(isDisplayed()));
                return;
            } catch (Exception e) {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException ignored) {}
            }
        }
        onView(withId(viewId)).check(matches(isDisplayed()));
    }

    /**
     * Helper to poll until at least one of the specified view IDs is displayed.
     */
    private void waitForAnyView(long timeoutMillis, int... viewIds) {
        long endTime = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < endTime) {
            for (int viewId : viewIds) {
                try {
                    onView(withId(viewId)).check(matches(isDisplayed()));
                    return;
                } catch (Exception ignored) {}
            }
            try {
                Thread.sleep(150);
            } catch (InterruptedException ignored) {}
        }
        onView(anyOf(withId(viewIds[0]), withId(viewIds.length > 1 ? viewIds[1] : viewIds[0]))).check(matches(isDisplayed()));
    }
}
