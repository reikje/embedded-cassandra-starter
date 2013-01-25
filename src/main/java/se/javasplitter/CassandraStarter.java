package se.javasplitter;

import org.apache.cassandra.config.ConfigurationException;
import org.apache.thrift.transport.TTransportException;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utility class using in tests to properly start up and shut down cassandra.
 *
 * @author rschatz, 2012-11-08
 */
public final class CassandraStarter {

    private CassandraStarter() { }

    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    public static void main(final String... args) {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("Please specify the location of a cassandra.yml file");
        }

        final String fileName = args[0];
        final File file = new File(fileName);
        if (!file.isFile()) {
            throw new IllegalArgumentException(String.format("%s does not exist or not accessible", fileName));
        }

        CassandraStarter.startNewEmbeddedCassandra(file);
    }

    private static synchronized void startNewEmbeddedCassandra(final File configurationFile) {
        if (!STARTED.get()) {
            try {
                BetterEmbeddedCassandraServerHelper.startEmbeddedCassandra(configurationFile);
            } catch (TTransportException e) {
                throw new IllegalStateException("Unable to start Cassandra.", e);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to start Cassandra.", e);
            } catch (ConfigurationException e) {
                throw new IllegalStateException("Unable to start Cassandra.", e);
            }
            STARTED.set(true);
        }
    }
}
