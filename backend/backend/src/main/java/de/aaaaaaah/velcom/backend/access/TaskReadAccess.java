package de.aaaaaaah.velcom.backend.access;

import de.aaaaaaah.velcom.backend.access.entities.Task;
import de.aaaaaaah.velcom.backend.storage.db.DatabaseStorage;
import de.aaaaaaah.velcom.backend.storage.repo.RepoStorage;

import java.util.List;
import java.util.Objects;

public class TaskReadAccess {

    protected final DatabaseStorage databaseStorage;

    public TaskReadAccess(DatabaseStorage databaseStorage) {
        this.databaseStorage = Objects.requireNonNull(databaseStorage);
    }

    public List<Task> getTasksSorted() {
        return null;
    }

}
