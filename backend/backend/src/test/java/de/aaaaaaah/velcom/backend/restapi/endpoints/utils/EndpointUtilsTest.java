package de.aaaaaaah.velcom.backend.restapi.endpoints.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.aaaaaaah.velcom.backend.util.Pair;
import java.util.List;
import org.junit.jupiter.api.Test;

class EndpointUtilsTest {

	@Test
	void parseValidColonSeparatedArgsCorrectly() {
		String args = "hello:world:out:there::goodbye:and:farewell";
		List<Pair<String, List<String>>> parsedArgs = EndpointUtils.parseColonSeparatedArgs(args);
		assertThat(parsedArgs).isEqualTo(
			List.of(
				new Pair<>("hello", List.of("world", "out", "there")),
				new Pair<>("goodbye", List.of("and", "farewell"))
			)
		);
	}

	@Test
	void throwErrorOnInvalidColonSeparatedArgs() {
		String args = "the:second:section:has:too:few:elements::see?::the:third:section:is:fine:though";
		assertThrows(ArgumentParseException.class, () -> EndpointUtils.parseColonSeparatedArgs(args));
	}
}
