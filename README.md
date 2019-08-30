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

### Create Employee
```
curl -X POST \
  http://localhost:9000/api/employees \
  -H 'cache-control: no-cache' \
  -H 'content-type: application/json' \
  -H 'postman-token: 7e3f3675-c27d-3dd2-df54-a32f276c7b41' \
  -d '{
	"id": "128",
	"name": "Shivam",
	"gender": "M",
	"doj": "2017-01-16",
	"pfn": "PFKN110"
}'
```

### Verify Kafka
```
$ /opt/kafka_2.12-1.0.0/bin/kafka-topics.sh --zookeeper localhost:2181 --list
__consumer_offsets
employee

$ /opt/kafka_2.12-1.0.0/bin/kafka-console-consumer.sh --topic employee --bootstrap-server localhost:9092 --from-beginning
{"gender":"M","name":"Shivam","pfn":"PFKN110","id":"128","type":"EmployeeAdded","doj":"2017-01-16"}
```

### Verify Cassandra
```
$ /opt/apache-cassandra-3.11.4/bin/cqlsh localhost 4000
cqlsh> USE simplelms;
cqlsh:simplelms> select * from messages ;

 persistence_id                                 | partition_nr | sequence_nr | timestamp                            | timebucket | used | event                                                                                                                                                                                                                                                                                                                                      | event_manifest | message | meta | meta_ser_id | meta_ser_manifest | ser_id  | ser_manifest                                              | tag1                                                      | tag2 | tag3 | writer_uuid
------------------------------------------------+--------------+-------------+--------------------------------------+------------+------+--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+----------------+---------+------+-------------+-------------------+---------+-----------------------------------------------------------+-----------------------------------------------------------+------+------+--------------------------------------
                  EmployeePersistenceEntity|128 |            0 |           1 | 9757cab0-cb0c-11e9-91c9-d1a5d92dc8d6 |   20190830 | True |                                                                                                                                                                                 0x7b226964223a22313238222c226e616d65223a2253686976616d222c2267656e646572223a224d222c22646f6a223a22323031372d30312d3136222c2270666e223a2250464b4e313130227d |                |    null | null |        null |              null | 1000004 | com.codingkapoor.employee.persistence.write.EmployeeAdded | com.codingkapoor.employee.persistence.write.EmployeeEvent | null | null | 5ecbca45-9b79-46c5-a858-5c97d9ed1da2
```

### Verify Mysql
```
$ mysql -u codingkapoor -p
mysql> USE simplelms;
mysql> SELECT * FROM employee;
+-----+--------+--------+------------+---------+
| ID  | NAME   | GENDER | DOJ        | PFN     |
+-----+--------+--------+------------+---------+
| 128 | Shivam | M      | 2017-01-16 | PFKN110 |
+-----+--------+--------+------------+---------+
1 row in set (0.00 sec)
```
