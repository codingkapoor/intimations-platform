# intimations-platform
Platform for Intimations.

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
$mysql -u codingkapoor -p
mysql > CREATE DATABASE intimations;
```

### Create Tables
```
CREATE TABLE `employee` (
  `ID` bigint(20) NOT NULL,
  `NAME` text NOT NULL,
  `GENDER` text NOT NULL,
  `DOJ` date NOT NULL,
  `DESIGNATION` text NOT NULL,
  `PFN` varchar(64) NOT NULL,
  `IS_ACTIVE` tinyint(1) NOT NULL,
  `PHONE` text NOT NULL,
  `EMAIL` text NOT NULL,
  `CITY` text NOT NULL,
  `STATE` text NOT NULL,
  `COUNTRY` text NOT NULL,
  `EARNED_LEAVES` int(11) NOT NULL,
  `SICK_LEAVES` int(11) NOT NULL,
  PRIMARY KEY (`ID`),
  UNIQUE KEY `PFN` (`PFN`)
) 

CREATE TABLE `intimation` (
  `EMP_ID` bigint(20) NOT NULL,
  `REASON` text NOT NULL,
  `LATEST_REQUEST_DATE` date NOT NULL,
  `LAST_MODIFIED` text NOT NULL,
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`ID`)
)

CREATE TABLE `request` (
  `DATE` int(11) NOT NULL,
  `MONTH` int(11) NOT NULL,
  `YEAR` int(11) NOT NULL,
  `FIRST_HALF` text NOT NULL,
  `SECOND_HALF` text NOT NULL,
  `INTIMATION_ID` bigint(20) NOT NULL,
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`ID`),
  KEY `INTIMATION_FK` (`INTIMATION_ID`),
  CONSTRAINT `INTIMATION_FK` FOREIGN KEY (`INTIMATION_ID`) REFERENCES `intimation` (`ID`) ON DELETE CASCADE ON UPDATE NO ACTION
)

CREATE TABLE `holiday` (
  `DATE` varchar(255) NOT NULL,
  `OCCASION` text NOT NULL,
  PRIMARY KEY (`DATE`)
)
```

## Dev
### Clone Repo
```
$ git clone git@github.com:codingkapoor/intimations-platform.git
```

### Start All Services
```
$ cd intimations-platform
$ sbt
sbt> runAll

$ curl http://localhost:9008/services
[
  {
    "name": "cas_native",
    "url": "tcp://127.0.0.1:4000/cas_native",
    "portName": null
  },
  {
    "name": "kafka_native",
    "url": "tcp://localhost:9092/kafka_native",
    "portName": null
  },
  {
    "name": "employee",
    "url": "http://127.0.0.1:53823",
    "portName": null
  },
  {
    "name": "employee",
    "url": "http://127.0.0.1:53823",
    "portName": "http"
  }
]
```

### Create Employee
```
curl -X POST \
  http://localhost:9000/api/employees \
  -H 'Accept: */*' \
  -H 'Accept-Encoding: gzip, deflate' \
  -H 'Cache-Control: no-cache' \
  -H 'Connection: keep-alive' \
  -H 'Content-Length: 427' \
  -H 'Content-Type: application/json' \
  -H 'cache-control: no-cache' \
  -d '{
    "id": 128,
    "name": "Shivam Kapoor",
    "gender": "M",
    "doj": "2017-01-16",
    "designation": "Sr. Big Data Developer",
    "pfn": "PYKRP00452140000000084",
    "contactInfo": {
    	"phone": "+91-9663006554",
    	"email": "mail@shivamkapoor.com"
    },
    "location": {
    	"city": "Bangalore",
    	"state": "Karnataka",
    	"country": "India"
    },
    "leaves": {
    	"earned": 11,
    	"sick": 2
    }
}'
```

### Verify Kafka
```
$ /opt/kafka_2.12-1.0.0/bin/kafka-topics.sh --zookeeper localhost:2181 --list
__consumer_offsets
employee

$ /opt/kafka_2.12-1.0.0/bin/kafka-console-consumer.sh --topic employee --bootstrap-server localhost:9092 --from-beginning
{"leaves":{"earned":11,"sick":2},"contactInfo":{"phone":"+91-9663006554","email":"mail@shivamkapoor.com"},"gender":"M","name":"Shivam Kapoor","pfn":"PYKRP00452140000000084","location":{"city":"Bangalore","state":"Karnataka","country":"India"},"designation":"Sr. Big Data Developer","id":128,"type":"EmployeeAddedKafkaEvent","isActive":true,"doj":"2017-11-16"}
```

### Verify Cassandra
```
$ /opt/apache-cassandra-3.11.4/bin/cqlsh localhost 4000
cqlsh> USE intimations;
cqlsh:intimations> select * from messages ;

 persistence_id                                 | partition_nr | sequence_nr | timestamp                            | timebucket | used | event                                                                                                                                                                                                                                                                                                                                      | event_manifest | message | meta | meta_ser_id | meta_ser_manifest | ser_id  | ser_manifest                                              | tag1                                                      | tag2 | tag3 | writer_uuid
------------------------------------------------+--------------+-------------+--------------------------------------+------------+------+--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+----------------+---------+------+-------------+-------------------+---------+-----------------------------------------------------------+-----------------------------------------------------------+------+------+--------------------------------------
                  EmployeePersistenceEntity|128 |            0 |           1 | 9757cab0-cb0c-11e9-91c9-d1a5d92dc8d6 |   20190830 | True |                                                                                                                                                                                 0x7b226964223a22313238222c226e616d65223a2253686976616d222c2267656e646572223a224d222c22646f6a223a22323031372d30312d3136222c2270666e223a2250464b4e313130227d |                |    null | null |        null |              null | 1000004 | com.codingkapoor.employee.persistence.write.EmployeeAdded | com.codingkapoor.employee.persistence.write.EmployeeEvent | null | null | 5ecbca45-9b79-46c5-a858-5c97d9ed1da2
    /sharding/kafkaProducer-employeeCoordinator |            0 |           1 | 947e2530-cb09-11e9-91c9-d1a5d92dc8d6 |   20190830 | True |                                                                                                                           0x0a65616b6b612e7463703a2f2f656d706c6f7965652d696d706c2d6170706c69636174696f6e403132372e302e302e313a33383833352f73797374656d2f7368617264696e672f6b61666b6150726f64756365722d656d706c6f79656523333832323635343635 |                |    null | null |        null |              null |      13 |                                                        AB |                                                      null | null | null | c39f782f-5a96-4628-9568-060d01ad6a93
    /sharding/kafkaProducer-employeeCoordinator |            0 |           2 | 94850300-cb09-11e9-91c9-d1a5d92dc8d6 |   20190830 | True |                                                                                                     0x0a0973696e676c65746f6e1265616b6b612e7463703a2f2f656d706c6f7965652d696d706c2d6170706c69636174696f6e403132372e302e302e313a33383833352f73797374656d2f7368617264696e672f6b61666b6150726f64756365722d656d706c6f79656523333832323635343635 |                |    null | null |        null |              null |      13 |                                                        AF |                                                      null | null | null | c39f782f-5a96-4628-9568-060d01ad6a93
    /sharding/EmployeeEventProcessorCoordinator |            0 |           1 | 947e2531-cb09-11e9-91c9-d1a5d92dc8d6 |   20190830 | True |                                                                                                                       0x0a67616b6b612e7463703a2f2f656d706c6f7965652d696d706c2d6170706c69636174696f6e403132372e302e302e313a33383833352f73797374656d2f7368617264696e672f456d706c6f7965654576656e7450726f636573736f72232d31323535303739333037 |                |    null | null |        null |              null |      13 |                                                        AB |                                                      null | null | null | b12e8f2c-485d-4c51-a7b3-7da9d66f3949
    /sharding/EmployeeEventProcessorCoordinator |            0 |           2 | 94848dd0-cb09-11e9-91c9-d1a5d92dc8d6 |   20190830 | True | 0x0a39636f6d2e636f64696e676b61706f6f722e656d706c6f7965652e70657273697374656e63652e77726974652e456d706c6f7965654576656e741267616b6b612e7463703a2f2f656d706c6f7965652d696d706c2d6170706c69636174696f6e403132372e302e302e313a33383833352f73797374656d2f7368617264696e672f456d706c6f7965654576656e7450726f636573736f72232d31323535303739333037 |                |    null | null |        null |              null |      13 |                                                        AF |                                                      null | null | null | b12e8f2c-485d-4c51-a7b3-7da9d66f3949
 /sharding/EmployeePersistenceEntityCoordinator |            0 |           1 | 947e2532-cb09-11e9-91c9-d1a5d92dc8d6 |   20190830 | True |                                                                                                                     0x0a68616b6b612e7463703a2f2f656d706c6f7965652d696d706c2d6170706c69636174696f6e403132372e302e302e313a33383833352f73797374656d2f7368617264696e672f456d706c6f79656550657273697374656e6365456e74697479232d3231303737303339 |                |    null | null |        null |              null |      13 |                                                        AB |                                                      null | null | null | d4fbac44-e315-4f76-8fe3-7184f874cb87
 /sharding/EmployeePersistenceEntityCoordinator |            0 |           2 | 975050a0-cb0c-11e9-91c9-d1a5d92dc8d6 |   20190830 | True |                                                                                                             0x0a0239351268616b6b612e7463703a2f2f656d706c6f7965652d696d706c2d6170706c69636174696f6e403132372e302e302e313a33383833352f73797374656d2f7368617264696e672f456d706c6f79656550657273697374656e6365456e74697479232d3231303737303339 |                |    null | null |        null |              null |      13 |                                                        AF |                                                      null | null | null | d4fbac44-e315-4f76-8fe3-7184f874cb87

```

### Verify Mysql
```
$ mysql -u codingkapoor -p
mysql> USE intimations;
mysql> SELECT * FROM employee;
+-----+---------------+--------+------------+--------------------+------------------------+-----------+----------------+-------------------------+-----------+-----------+---------+---------------+-------------+
| ID  | NAME          | GENDER | DOJ        | DESIGNATION        | PFN                    | IS_ACTIVE | PHONE          | EMAIL                   | CITY      | STATE     | COUNTRY | EARNED_LEAVES | SICK_LEAVES |
+-----+---------------+--------+------------+--------------------+------------------------+-----------+----------------+-------------------------+-----------+-----------+---------+---------------+-------------+
| 128 | Shivam Kapoor | M      | 2017-11-16 | Sr. Big Data Developer | PYKRP00452140000000084 |         1 | +91-9663006554 | mail@shivamkapoor.com | Bangalore | Karnataka | India   |            11 |           2 |
+-----+---------------+--------+------------+--------------------+------------------------+-----------+----------------+-------------------------+-----------+-----------+---------+---------------+-------------+

1 row in set (0.00 sec)
```

## Api Docs
### Add Employee
#### Privilege
Administrator
#### Request
```
curl -X POST \
  http://localhost:9000/api/employees \
  -H 'Accept: */*' \
  -H 'Accept-Encoding: gzip, deflate' \
  -H 'Cache-Control: no-cache' \
  -H 'Connection: keep-alive' \
  -H 'Content-Length: 427' \
  -H 'Content-Type: application/json' \
  -H 'cache-control: no-cache' \
  -d '{
    "id": 128,
    "name": "Shivam Kapoor",
    "gender": "M",
    "doj": "2017-01-16",
    "designation": "Sr. Big Data Developer",
    "pfn": "PYKRP00452140000000084",
    "contactInfo": {
    	"phone": "+91-9663006554",
    	"email": "mail@shivamkapoor.com"
    },
    "location": {
    	"city": "Bangalore",
    	"state": "Karnataka",
    	"country": "India"
    },
    "leaves": {
    	"earned": 11,
    	"sick": 2
    }
}'
```
#### Response
`200 OK`

### Get Employee
#### Privilege
Employee
#### Request
```
curl -X GET \
  http://localhost:9000/api/employees/128 
```
#### Response
```
200 OK

{
    "id": 128,
    "name": "Shivam Kapoor",
    "gender": "M",
    "doj": "2019-11-16",
    "designation": "Sr. Big Data Developer",
    "pfn": "PYKRP00452140000000084",
    "isActive": true,
    "contactInfo": {
        "phone": "+91-9663006554",
        "email": "mail@shivamkapoor.com"
    },
    "location": {
        "city": "Bangalore",
        "state": "Karnataka",
        "country": "India"
    },
    "leaves": {
        "earned": 11,
        "sick": 2
    }
}
```
### Get Employees
#### Privilege
Administrator
#### Request
```
curl -X GET \
  http://localhost:9000/api/employees 
```
#### Response
```
200 OK

[
    {
        "id": 128,
        "name": "Shivam Kapoor",
        "gender": "M",
        "doj": "2017-11-16",
        "designation": "Sr. Big Data Developer",
        "pfn": "PYKRP00452140000000084",
        "isActive": true,
        "contactInfo": {
            "phone": "+91-9663006554",
            "email": "mail@shivamkapoor.com"
        },
        "location": {
            "city": "Bangalore",
            "state": "Karnataka",
            "country": "India"
        },
        "leaves": {
            "earned": 11,
            "sick": 2
        }
    }
]
```
### Terminate Employee
#### Privilege
Administrator
#### Request
```
curl -X PUT \
  http://localhost:9000/api/employees/128/terminate \
```
#### Response
```
200 OK
```

A terminated employee is not an active member of an organization. Hence, `isActive` is set to `false`.

```
curl -X GET \
  http://localhost:9000/api/employees \
  
[
    {
        "id": 128,
        "name": "Shivam Kapoor",
        "gender": "M",
        "doj": "2017-11-16",
        "designation": "Sr. Big Data Developer",
        "pfn": "PYKRP00452140000000084",
        "isActive": false,
        "contactInfo": {
            "phone": "+91-9663006554",
            "email": "mail@shivamkapoor.com"
        },
        "location": {
            "city": "Bangalore",
            "state": "Karnataka",
            "country": "India"
        },
        "leaves": {
            "earned": 11,
            "sick": 2
        }
    }
]
```
### Delete Employee
This also deletes all the intimations and requests as a cascading delete effect.
#### Privilege
Administrator
#### Request
```
curl -X DELETE \
  http://localhost:9000/api/employees/128 \
```
#### Response
```
200 OK
```
### Create Intimation
#### Privilege
Employee
#### Request
```
curl -X POST \
  http://localhost:9000/api/employees/128/intimations \
  -H 'Accept: */*' \
  -H 'Accept-Encoding: gzip, deflate' \
  -H 'Cache-Control: no-cache' \
  -H 'Connection: keep-alive' \
  -H 'Content-Length: 152' \
  -H 'Content-Type: application/json' \
  -H 'cache-control: no-cache' \
  -d '{
	"reason": "Need to attend to some personal work. Will be available across all channels.",
	"requests": [
		{
			"date": "2019-11-20",
			"firstHalf": "WFO",
			"secondHalf": "WFH"
		}
	]
}'
```
#### Response
```
200 OK
```
### Get Intimations By Month
This returns both active as well as inactive intimations for a given month for an employee.
#### Privilege
Employee
#### Request
```
curl -X GET \
  'http://localhost:9000/api/employees/128/intimations?month=10&year=2019' \
```
#### Response
```
200 OK

[
    {
        "empId": 128,
        "reason": "Need to attend to some personal work. Will be available across all channels.",
        "requests": [
            {
                "date": "2019-11-20",
                "firstHalf": "WFO",
                "secondHalf": "WFH"
            }
        ]
    }
]
```
### Get Active Intimations
This returns all active intimations from all employees.
#### Privilege
Employee
#### Request
```
curl -X GET \
  http://localhost:9000/api/employees/intimations \
```
#### Response
```
200 OK

[
    {
        "empId": 128,
        "empName": "Shivam Kapoor",
        "reason": "Need to attend to some personal work.",
        "lastModified": "2019-11-19T09:14:41.713",
        "requests": [
            {
                "date": "2019-11-20",
                "firstHalf": "WFO",
                "secondHalf": "WFH"
            }
        ]
    }
]
```
### Update Intimation
Only requests in the future can be modified.
#### Privilege
Employee
#### Request
```
curl -X PUT \
  http://localhost:9000/api/employees/128/intimations \
  -H 'Accept: */*' \
  -H 'Accept-Encoding: gzip, deflate' \
  -H 'Cache-Control: no-cache' \
  -H 'Connection: keep-alive' \
  -H 'Content-Length: 154' \
  -H 'Content-Type: application/json' \
  -H 'cache-control: no-cache' \
  -d '{
	"reason": "Not feeling well.",
	"requests": [
		{
			"date": "2019-11-20",
			"firstHalf": "WFO",
			"secondHalf": "Leave"
		}
	]
}'
```
#### Response
```
200 OK
```
### Cancel Intimation
Requests in the past don't get cancelled.
#### Privilege
Employee
#### Request
```
curl -X PUT \
  http://localhost:9000/api/employees/129/intimations/cancel \
```
#### Response
```
200 OK
```
### Get Leaves
#### Privilege
Employee
#### Request
```
curl -X GET \
  http://localhost:9000/api/employees/128/leaves \
```
#### Response
```
{
    "earned": 12,
    "sick": 8
}
```
