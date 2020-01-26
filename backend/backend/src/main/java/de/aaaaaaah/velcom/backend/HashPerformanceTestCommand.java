package de.aaaaaaah.velcom.backend;

import de.aaaaaaah.velcom.backend.access.token.hashalgorithm.Argon2Algorithm;
import de.mkammerer.argon2.Argon2Factory;
import de.mkammerer.argon2.Argon2Helper;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

/**
 * A CLI command that runs a small argon2 hash benchmark.
 */
public class HashPerformanceTestCommand extends Command {

	public HashPerformanceTestCommand() {
		super("hashPerformance",
			"Runs a hash computation for X millis to give you a baseline for the"
				+ " chosen password encryption rounds."
		);
	}

	@Override
	public void configure(Subparser subparser) {
		subparser.addArgument("maxMillis")
			.dest("maxMillis")
			.type(Long.class)
			.required(true)
			.help("The amount of milliseconds hashing is allowed to take");
		subparser.addArgument("maxMemory")
			.dest("maxMemory")
			.type(Integer.class)
			.required(true)
			.help("The maximum amount of memory in Kibibytes the hash function might use");
	}

	@Override
	public void run(Bootstrap<?> bootstrap, Namespace namespace) {
		long maxMillis = namespace.getLong("maxMillis");
		int maxMemoryKiB = namespace.getInt("maxMemory");

		int iterations = Argon2Helper.findIterations(
			Argon2Factory.create(),
			maxMillis,
			maxMemoryKiB,
			Argon2Algorithm.parallelism
		);
		System.out.println(
			Math.max(1, iterations) + " iteration(s) seem to fit into " + maxMillis
				+ " milliseconds!"
		);
	}
}
