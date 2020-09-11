package de.aaaaaaah.velcom.backend.restapi.jsonobjects;

import java.util.UUID;

public class JsonRun {

	private final UUID id;
	private final String author;
	private final String runnerName;
	private final String runnerInfo;
	private final long startTime;
	private final long stopTime;
	private final JsonSource source;
	private final JsonResult result;

	public JsonRun(UUID id, String author, String runnerName, String runnerInfo, long startTime,
		long stopTime, JsonSource source, JsonResult result) {

		this.id = id;
		this.author = author;
		this.runnerName = runnerName;
		this.runnerInfo = runnerInfo;
		this.startTime = startTime;
		this.stopTime = stopTime;
		this.source = source;
		this.result = result;
	}

	public UUID getId() {
		return id;
	}

	public String getAuthor() {
		return author;
	}

	public String getRunnerName() {
		return runnerName;
	}

	public String getRunnerInfo() {
		return runnerInfo;
	}

	public long getStartTime() {
		return startTime;
	}

	public long getStopTime() {
		return stopTime;
	}

	public JsonSource getSource() {
		return source;
	}

	public JsonResult getResult() {
		return result;
	}
}
