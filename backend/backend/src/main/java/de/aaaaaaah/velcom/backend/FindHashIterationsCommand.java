package de.aaaaaaah.velcom.backend;

import de.mkammerer.argon2.Argon2Factory;
import de.mkammerer.argon2.Argon2Helper;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

/**
 * A CLI command that runs a small argon2 hash benchmark.
 */
public class FindHashIterationsCommand extends Command {

	public FindHashIterationsCommand() {
		super(
			"findHashIterations",
			"Determine the amount of iterations that most closely matches the specified maximum"
				+ " time without going over."
		);
	}

	@Override
	public void configure(Subparser subparser) {
		subparser.addArgument("hashTime")
			.dest("hashTime")
			.type(Long.class)
			.required(true)
			.help("The maximum amount of time (in ms) that each call can afford");
		subparser.addArgument("hashMemory")
			.dest("hashMemory")
			.type(Integer.class)
			.required(true)
			.help("The maximum amount of memory (in KiB) that each call can afford");
		subparser.addArgument("hashParallelism")
			.dest("hashParallelism")
			.type(Integer.class)
			.required(true)
			.help("The maximum number of threads that can be initiated by each call");
	}

	@Override
	public void run(Bootstrap<?> bootstrap, Namespace namespace) {
		long hashTime = namespace.getLong("hashTime");
		int hashMemory = namespace.getInt("hashMemory");
		int hashParallelism = namespace.getInt("hashParallelism");

		System.out.println("This may take a while...");

		int iterations = Argon2Helper.findIterations(
			Argon2Factory.create(),
			hashTime,
			hashMemory,
			hashParallelism
		);

		System.out.println(Math.max(1, iterations) + " iteration(s) seem to fit into "
			+ hashTime + " milliseconds!");
	}
}
