package de.feelix.sierra.utilities;

import lombok.Getter;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The UniqueRunnableExecutor class provides a mechanism for executing a Runnable task only once within a specified
 * interval.
 * It uses a ConcurrentHashMap to store the execution status and last execution time of each task.
 */
@Getter
public class UniqueRunnableExecutor {

    /**
     * A ConcurrentHashMap that stores the execution status of tasks identified by UUID keys.
     * Each task is represented by an AtomicBoolean value indicating whether it has been executed or not.
     */
    private final ConcurrentHashMap<UUID, AtomicBoolean> flagOfExecutedTask = new ConcurrentHashMap<>();

    /**
     * Executes a task only once within a specified interval.
     * If the task has not been executed within the interval, it will be executed.
     */
    private final ConcurrentHashMap<UUID, AtomicLong> lastExecutedTimeMap = new ConcurrentHashMap<>();

    /**
     * The interval, in milliseconds, at which a task can be executed.
     */
    private final long taskExecutionIntervalMillis = 1000; // 1 second

    /**
     * Executes a task only once within a specified interval.
     * If the task has not been executed within the interval, it will be executed.
     *
     * @param taskKey The key associated with the task.
     * @param task    The task to be executed.
     */
    public void executeTaskOnce(UUID taskKey, Runnable task) {
        long       currentTime      = System.currentTimeMillis();
        AtomicLong lastExecutedTime = getLastExecutionTime(taskKey);
        if (!isTaskExecutedWithinInterval(currentTime, lastExecutedTime)) {
            AtomicBoolean taskExecutionStatus = getTaskExecutionStatus(taskKey);
            executeAndMarkTaskIfNotExecutedBefore(task, currentTime, lastExecutedTime, taskExecutionStatus);
        }
    }

    /**
     * Executes a task only once within a specified interval if it has not been executed before.
     *
     * @param task                The task to be executed.
     * @param currentTime         The current time in milliseconds.
     * @param lastExecutedTime    The AtomicLong representing the last time the task was executed.
     * @param taskExecutionStatus The AtomicBoolean representing the execution status of the task.
     */
    private void executeAndMarkTaskIfNotExecutedBefore(
        Runnable task, long currentTime, AtomicLong lastExecutedTime, AtomicBoolean taskExecutionStatus) {
        if (!taskExecutionStatus.get()) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (taskExecutionStatus) {
                if (!taskExecutionStatus.get()) {
                    task.run();
                    taskExecutionStatus.set(true);
                    lastExecutedTime.set(currentTime); // Update the last execution time
                }
            }
        }
    }

    /**
     * Retrieves the execution status of a task identified by the given task key.
     *
     * @param taskKey The key associated with the task.
     * @return The AtomicBoolean representing the execution status of the task. If the execution status of the task
     * has not been set previously, a new AtomicBoolean with a default value of false is created and returned.
     */
    private AtomicBoolean getTaskExecutionStatus(UUID taskKey) {
        return flagOfExecutedTask.computeIfAbsent(taskKey, k -> new AtomicBoolean(false));
    }

    /**
     * Retrieves the last execution time for a task identified by the given key.
     *
     * @param taskKey The key associated with the task.
     * @return The AtomicLong representing the last execution time of the task. If the task has not been executed
     * before,
     * a new AtomicLong with a default value of 0 is created and returned.
     */
    private AtomicLong getLastExecutionTime(UUID taskKey) {
        return lastExecutedTimeMap.computeIfAbsent(taskKey, k -> new AtomicLong(0));
    }

    /**
     * Checks if a task has been executed within a specified interval of time.
     *
     * @param currentTime      The current time in milliseconds.
     * @param lastExecutedTime The AtomicLong representing the last time the task was executed.
     * @return True if the task has been executed within the interval, false otherwise.
     */
    private boolean isTaskExecutedWithinInterval(long currentTime, AtomicLong lastExecutedTime) {
        return currentTime - lastExecutedTime.get() < taskExecutionIntervalMillis;
    }
}
