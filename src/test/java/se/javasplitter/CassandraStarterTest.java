package se.javasplitter;

import org.junit.Test;

/**
 * Starts Cassandra.
 *
 * @author rschatz, 2012-11-22
 */
public class CassandraStarterTest {

    @Test
    public void testStarting() {
        final String config = Thread.currentThread().getContextClassLoader().getResource("cassandra.yml").getFile();
        CassandraStarter.main(config);
    }
}
