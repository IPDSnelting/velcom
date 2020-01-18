package de.aaaaaaah.velcom.backend.prototype;

import de.aaaaaaah.velcom.backend.ServerMain;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public class AddRepoTest {

	public static void main(String[] args) throws Exception {
		startBackend();

		Thread.sleep(2000);

		System.out.println("Started backend! Starting runner...");

		Thread.sleep(2000);

		startRunner();

		Thread.sleep(2000);

		System.out.println("Started runner! Sending request...");

		Thread.sleep(2000);

		sendHttpRequest();
	}

	private static void startRunner() throws IOException {
		Path curDir = Paths.get("").toAbsolutePath();

		Path runnerDir = curDir.getParent().resolve("runner");

		String config = runnerDir.resolve("src").resolve("main").resolve("resources")
			.resolve("Example_config.json").toAbsolutePath().toString();

		String jar = runnerDir.resolve("target").resolve("runner.jar").toAbsolutePath().toString();

		Process process = new ProcessBuilder(
			ProcessHandle.current().info().command().orElseThrow(),
			"-jar",
			jar,
			config
		)
			.directory(runnerDir.resolve("target").toFile())
			.inheritIO()
			.start();

		Runtime.getRuntime()
			.addShutdownHook(new Thread(process::destroyForcibly, "KillRunnerHook"));
	}

	private static void startBackend() throws Exception {
		Path configPath = Paths.get("src", "main",
			"resources", "SampleConfig.yml");

		ServerMain.main(new String[]{
			"server", configPath.toString()
		});
	}

	private static void sendHttpRequest()
		throws URISyntaxException, IOException, InterruptedException {
		HttpClient client = HttpClient.newHttpClient();

		URI uri = new URI("http://0.0.0.0:8080/repo");

		String bla = "admin:12345";
		String blub = "Basic " + Base64.getEncoder()
			.encodeToString(bla.getBytes(StandardCharsets.UTF_8));

		HttpRequest request = HttpRequest.newBuilder(uri)
			.header("Authorization", blub)
			.header("Content-type", "application/json")
			.POST(BodyPublishers.ofString("{\n"
				+ "\"name\":\"tiny_repo\",\n"
				+ "\"remote_url\":\"https://github.com/kwerber/tiny_repo.git\",\n"
				+ "\"token\":\"45678\"\n"
				+ "}"))
			.build();

		HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

		System.out.println("status: " + response.statusCode());
		System.out.println("got response: " + response.body());
	}

}
