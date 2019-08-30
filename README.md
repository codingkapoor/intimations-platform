# simple-lms-platform
Platform for simple lms.

## Scope
1. Maintains list of all the employees.
2. Maintains records of earned & sick leaves of all the employees. This includes automatically updating earned and sick leaves every month per employee and deducting the same when employee applies for leaves. On the event of cancellation of leaves, they are credited back.

## Setup Mysql
### Create User
```
$ mysql -u root -p
mysql> CREATE USER 'codingkapoor'@'localhost' IDENTIFIED BY 'codingkapoor';
mysql> GRANT ALL PRIVILEGES ON * . * TO 'codingkapoor'@'localhost';
```

### Create Database
```
$mysql -u codingkapoor -u
mysql > CREATE DATABASE simplelms;
```

## Dev
### Clone Repo
```
$ git clone git@github.com:codingkapoor/simple-lms-platform.git
```

### Start All Services
```
$ cd simple-lms-platform
$ sbt
sbt> runAll
```

### Verify Kafka
```
$ /opt/kafka_2.12-1.0.0/bin/kafka-topics.sh --zookeeper localhost:2181 --list
__consumer_offsets
employee

$ /opt/kafka_2.12-1.0.0/bin/kafka-console-consumer.sh --topic employee --bootstrap-server localhost:9092 --from-beginning

```

### Verify Cassandra
```
$ /opt/apache-cassandra-3.11.4/bin/cqlsh localhost 4000
cqlsh> USE simplelms;
cqlsh:simplelms> 
```

### Verify Mysql
```
$ mysql -u codingkapoor -p
mysql> USE simplelms;
mysql> SELECT * FROM employee;
```
