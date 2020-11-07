package de.aaaaaaah.velcom.backend.newaccess.shared;

import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.Task;
import de.aaaaaaah.velcom.backend.newaccess.taskaccess.entities.TaskId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

public class TaskCallbacks {

	private final Collection<Consumer<Task>> insertHandlers = new ArrayList<>();
	private final Collection<Consumer<TaskId>> deleteHandlers = new ArrayList<>();

	public void addInsertHandler(Consumer<Task> handler) {
		insertHandlers.add(handler);
	}

	public void addDeleteHandler(Consumer<TaskId> handler) {
		deleteHandlers.add(handler);
	}

	public void callInsertHandlers(Task task) {
		for (Consumer<Task> handler : insertHandlers) {
			handler.accept(task);
		}
	}

	public void callDeleteHandlers(TaskId taskId) {
		for (Consumer<TaskId> handler : deleteHandlers) {
			handler.accept(taskId);
		}
	}
}
