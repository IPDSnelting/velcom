package de.aaaaaaah.velcom.runner.revision.benchmarking;

import static org.assertj.core.api.Assertions.assertThat;

import de.aaaaaaah.velcom.runner.revision.benchmarking.BenchmarkFailureInformation;
import de.aaaaaaah.velcom.shared.util.systeminfo.LinuxSystemInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BenchmarkFailureInformationTest {

	private BenchmarkFailureInformation information;

	@BeforeEach
	void setUp() {
		information = new BenchmarkFailureInformation();
	}

	@Test
	void emptyGeneral() {
		assertThat(information.toString()).isEqualTo("");
	}

	@Test
	void addToGeneral() {
		information.addToGeneral("Hello", "World");
		assertThat(information.toString()).isEqualTo(
			"#############\n" +
				"## General ##\n" +
				"#############\n" +
				"Hello : World"
		);
	}

	@Test
	void alignsGeneral() {
		information.addToGeneral("Hello", "World");
		information.addToGeneral("You", "are pretty");
		assertThat(information.toString()).isEqualTo(
			"#############\n" +
				"## General ##\n" +
				"#############\n" +
				"Hello : World\n" +
				"You   : are pretty"
		);
	}

	@Test
	void addToGeneralAfterNewSection() {
		information.addToGeneral("Hello", "World");
		information.addSection("Hello", "World!");
		information.addToGeneral("You", "are pretty");
		assertThat(information.toString()).startsWith(
			"#############\n" +
				"## General ##\n" +
				"#############\n" +
				"Hello : World\n" +
				"You   : are pretty"
		);
	}

	@Test
	void addNewSection() {
		information.addSection("Hello", "World!");
		assertThat(information.toString()).isEqualTo(
			"###########\n" +
				"## Hello ##\n" +
				"###########\n" +
				"World!"
		);
	}

	@Test
	void addMachineInfo() {
		information.addMachineInfo(LinuxSystemInfo.getCurrent());
		assertThat(information.toString()).startsWith(
			"#############\n" +
				"## General ##\n" +
				"#############\n" +
				"Machine Info :"
		);
	}

	@Test
	void addEscapedArray() {
		information.addEscapedArrayToGeneral("Test", new String[]{
			"\"hello\" you there\n\t\n",
			"HEY! \n\""
		});
		assertThat(information.toString()).isEqualTo(
			"#############\n" +
				"## General ##\n" +
				"#############\n" +
				"Test : [\"\\\"hello\\\" you there\\n\\t\\n\", \"HEY! \\n\\\"\"]"
		);
	}
}