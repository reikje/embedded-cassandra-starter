### Description

A modified embedded Cassandra based on cassandra-unit. The main difference is that it uses the specified configuration
file (cassandra.yml) in place without making a copy of it in the temp directory.

### Start Command Example

java -cp "target/embedded-cassandra-starter-1.0-SNAPSHOT.jar" se.javasplitter.CassandraStarter /home/user/dev/embedded-cassandra-starter/src/test/resources/cassandra.yml