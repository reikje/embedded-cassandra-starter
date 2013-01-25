package se.javasplitter;

import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.factory.HFactory;
import org.apache.cassandra.config.ConfigurationException;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.db.commitlog.CommitLog;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.thrift.CassandraDaemon;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * todo: comment me
 *
 * @author rschatz, 2012-11-22
 */
public class BetterEmbeddedCassandraServerHelper {
    private static Logger log = LoggerFactory.getLogger(BetterEmbeddedCassandraServerHelper.class);

    public static final String DEFAULT_CASSANDRA_YML_FILE = "cu-cassandra.yaml";
    private static final String INTERNAL_CASSANDRA_KEYSPACE = "system";

    private static CassandraDaemon cassandraDaemon = null;
    static ExecutorService executor;
    private static String launchedYamlFile;

    public static void startEmbeddedCassandra(final File file) throws TTransportException, IOException, ConfigurationException {
        if (cassandraDaemon != null) {
            /* nothing to do Cassandra is already started */
            return;
        }

        checkConfigNameForRestart(file.getAbsolutePath());

        log.debug("Starting cassandra...");
        log.debug("Initialization needed");

        System.setProperty("cassandra.config", "file:" + file.getAbsolutePath());
        System.setProperty("cassandra-foreground", "true");

        cleanupAndLeaveDirs();
        final CountDownLatch startupLatch = new CountDownLatch(1);
        executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                cassandraDaemon = new CassandraDaemon();
                cassandraDaemon.activate();
                startupLatch.countDown();
            }
        });
        try {
            startupLatch.await(10, SECONDS);
        } catch (InterruptedException e) {
            log.error("Interrupted waiting for Cassandra daemon to start:", e);
            throw new AssertionError(e);
        }
    }

    private static void checkConfigNameForRestart(String yamlFile) {
        boolean wasPreviouslyLaunched = launchedYamlFile != null;
        if (wasPreviouslyLaunched && !launchedYamlFile.equals(yamlFile)) {
            throw new UnsupportedOperationException("We can't launch two Cassandra configurations in the same JVM instance");
        }
        launchedYamlFile = yamlFile;
    }

    /**
     * Now deprecated, previous version was not fully operating.
     * This is now an empty method, will be pruned in future versions.
     */
    @Deprecated
    public static void stopEmbeddedCassandra() {
        log.warn("EmbeddedCassandraServerHelper.stopEmbeddedCassandra() is now deprecated, " +
                "previous version was not fully operating");
    }

    /**
     * drop all keyspaces (expect system)
     */
    public static void cleanEmbeddedCassandra() {
        dropKeyspaces();
    }

    private static void dropKeyspaces() {
        String host = DatabaseDescriptor.getRpcAddress().getHostName();
        int port = DatabaseDescriptor.getRpcPort();
        log.debug("Cleaning cassandra keyspaces on " + host + ":" + port);
        Cluster cluster = HFactory.getOrCreateCluster("TestCluster", new CassandraHostConfigurator(host + ":" + port));
        /* get all keyspace */
        List<KeyspaceDefinition> keyspaces = cluster.describeKeyspaces();

        /* drop all keyspace except internal cassandra keyspace */
        for (KeyspaceDefinition keyspaceDefinition : keyspaces) {
            String keyspaceName = keyspaceDefinition.getName();

            if (!INTERNAL_CASSANDRA_KEYSPACE.equals(keyspaceName)) {
                cluster.dropKeyspace(keyspaceName);
            }
        }
    }

    private static void rmdir(final File dir) throws IOException {
        if (dir.exists()) {
            FileUtils.deleteRecursive(dir);
        }
    }

    private static void copy(final String configFile, final File directory) throws IOException {
        mkdir(directory);
        final InputStream is = new FileInputStream(configFile);
        final String fileName = configFile.substring(configFile.lastIndexOf("/") + 1);
        final File file = new File(directory + System.getProperty("file.separator") + fileName);
        final OutputStream out = new FileOutputStream(file);
        byte buf[] = new byte[1024];
        int len;
        while ((len = is.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.close();
        is.close();
    }

    private static void mkdir(File dir) throws IOException {
        FileUtils.createDirectory(dir);
    }

    private static void cleanupAndLeaveDirs() throws IOException {
        mkdirs();
        cleanup();
        mkdirs();
        CommitLog.instance.resetUnsafe(); // cleanup screws w/ CommitLog, this
        // brings it back to safe state
    }

    private static void cleanup() throws IOException {
        // clean up commitlog
        String[] directoryNames = {DatabaseDescriptor.getCommitLogLocation(),};
        for (String dirName : directoryNames) {
            File dir = new File(dirName);
            if (!dir.exists())
                throw new RuntimeException("No such directory: " + dir.getAbsolutePath());
            FileUtils.deleteRecursive(dir);
        }

        // clean up data directory which are stored as data directory/table/data
        // files
        for (String dirName : DatabaseDescriptor.getAllDataFileLocations()) {
            File dir = new File(dirName);
            if (!dir.exists())
                throw new RuntimeException("No such directory: " + dir.getAbsolutePath());
            FileUtils.deleteRecursive(dir);
        }
    }

    public static void mkdirs() {
        try {
            DatabaseDescriptor.createAllDirectories();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
