package de.aaaaaaah.velcom.backend;

import java.io.File;
import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.Collections;
import java.util.List;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.sshd.KeyCache;
import org.eclipse.jgit.transport.sshd.ProxyDataFactory;
import org.eclipse.jgit.transport.sshd.ServerKeyDatabase;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;

/**
 * A {@link SshdSessionFactory} that ignores server keys.
 */
public class KnownHostsIgnoringSshdFactory extends SshdSessionFactory {

	public KnownHostsIgnoringSshdFactory(KeyCache keyCache, ProxyDataFactory proxies) {
		super(keyCache, proxies);
	}

	@Override
	protected ServerKeyDatabase getServerKeyDatabase(File homeDir, File sshDir) {
		return new ServerKeyDatabase() {
			@Override
			public List<PublicKey> lookup(String connectAddress, InetSocketAddress remoteAddress,
				Configuration config) {
				return Collections.emptyList();
			}

			@Override
			public boolean accept(String connectAddress, InetSocketAddress remoteAddress,
				PublicKey serverKey, Configuration config, CredentialsProvider provider) {
				return true;
			}
		};
	}
}
