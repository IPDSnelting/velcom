package de.aaaaaaah.velcom.backend.restapi.logging;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.dropwizard.logging.filter.FilterFactory;
import org.kohsuke.MetaInfServices;

@MetaInfServices(FilterFactory.class)
@JsonTypeName("metrics-endpoint")
public class MetricsEndpointFilter implements FilterFactory<IAccessEvent> {

	@Override
	public Filter<IAccessEvent> build() {
		return new Filter<>() {
			@Override
			public FilterReply decide(IAccessEvent event) {
				if (event.getRequestURI().equals("/prometheusMetrics")) {
					return FilterReply.DENY;
				} else {
					return FilterReply.NEUTRAL;
				}
			}
		};
	}
}
