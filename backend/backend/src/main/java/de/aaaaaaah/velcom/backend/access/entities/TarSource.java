package de.aaaaaaah.velcom.backend.access.entities;

import java.nio.file.Path;
import java.util.Objects;

public class TarSource {

    private final String tarName;
    private final Path path;

    public TarSource(String tarName, Path tarPath) {
        this.tarName = Objects.requireNonNull(tarName);
        this.path = Objects.requireNonNull(tarPath);
    }

    public String getTarName() {
        return tarName;
    }

    public Path getTarPath() {
        return this.path;
    }

    @Override
    public String toString() {
        return "TarSource{" +
            "tarName='" + tarName + '\'' +
            '}';
    }

}
