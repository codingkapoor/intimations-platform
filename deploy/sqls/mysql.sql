CREATE DATABASE intimations_employee_schema;

USE intimations_employee_schema;

CREATE TABLE `employees` (
  `ID` bigint NOT NULL,
  `NAME` text NOT NULL,
  `GENDER` text NOT NULL,
  `DOJ` date NOT NULL,
  `DOR` date DEFAULT NULL,
  `DESIGNATION` text NOT NULL,
  `PFN` varchar(64) NOT NULL,
  `PHONE` text NOT NULL,
  `EMAIL` text NOT NULL,
  `CITY` text NOT NULL,
  `STATE` text NOT NULL,
  `COUNTRY` text NOT NULL,
  `EARNED_LEAVES` double NOT NULL,
  `CURRENT_YEAR_EARNED_LEAVES` double NOT NULL,
  `SICK_LEAVES` double NOT NULL,
  `EXTRA_LEAVES` double NOT NULL,
  `ROLES` text NOT NULL,
  PRIMARY KEY (`ID`)
);

CREATE TABLE `intimations` (
  `EMP_ID` bigint NOT NULL,
  `REASON` text NOT NULL,
  `LATEST_REQUEST_DATE` date NOT NULL,
  `LAST_MODIFIED` text NOT NULL,
  `ID` bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`ID`),
  KEY `EMP_FK` (`EMP_ID`),
  CONSTRAINT `EMP_FK` FOREIGN KEY (`EMP_ID`) REFERENCES `employees` (`ID`) ON DELETE CASCADE ON UPDATE NO ACTION
);

CREATE TABLE `requests` (
  `DATE` date NOT NULL,
  `FIRST_HALF` text NOT NULL,
  `SECOND_HALF` text NOT NULL,
  `INTIMATION_ID` bigint NOT NULL,
  `ID` bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`ID`),
  KEY `INTIMATION_FK` (`INTIMATION_ID`),
  CONSTRAINT `INTIMATION_FK` FOREIGN KEY (`INTIMATION_ID`) REFERENCES `intimations` (`ID`) ON DELETE CASCADE ON UPDATE NO ACTION
);

CREATE TABLE `journal` (
  `ordering` bigint NOT NULL AUTO_INCREMENT,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  `persistence_id` varchar(255) NOT NULL,
  `sequence_number` bigint NOT NULL,
  `message` blob NOT NULL,
  `tags` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`persistence_id`,`sequence_number`),
  UNIQUE KEY `ordering` (`ordering`),
  UNIQUE KEY `journal_ordering_idx` (`ordering`)
);

CREATE TABLE `read_side_offsets` (
  `read_side_id` varchar(255) NOT NULL,
  `tag` varchar(255) NOT NULL,
  `sequence_offset` bigint DEFAULT NULL,
  `time_uuid_offset` char(36) DEFAULT NULL,
  PRIMARY KEY (`read_side_id`,`tag`)
);

CREATE TABLE `snapshot` (
  `persistence_id` varchar(255) NOT NULL,
  `sequence_number` bigint NOT NULL,
  `created` bigint NOT NULL,
  `snapshot` blob NOT NULL,
  PRIMARY KEY (`persistence_id`,`sequence_number`)
);

CREATE DATABASE intimations_holiday_schema;

USE intimations_holiday_schema;

CREATE TABLE `holiday` (
  `DATE` date NOT NULL,
  `OCCASION` text NOT NULL,
  PRIMARY KEY (`DATE`)
);

CREATE TABLE `journal` (
  `ordering` bigint NOT NULL AUTO_INCREMENT,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  `persistence_id` varchar(255) NOT NULL,
  `sequence_number` bigint NOT NULL,
  `message` blob NOT NULL,
  `tags` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`persistence_id`,`sequence_number`),
  UNIQUE KEY `ordering` (`ordering`),
  UNIQUE KEY `journal_ordering_idx` (`ordering`)
);

CREATE TABLE `snapshot` (
  `persistence_id` varchar(255) NOT NULL,
  `sequence_number` bigint NOT NULL,
  `created` bigint NOT NULL,
  `snapshot` blob NOT NULL,
  PRIMARY KEY (`persistence_id`,`sequence_number`)
);

CREATE DATABASE intimations_notifier_schema;

USE intimations_notifier_schema;

CREATE TABLE `employees` (
  `EMP_ID` bigint NOT NULL,
  `EMP_NAME` text NOT NULL,
  `EXPO_TOKEN` text,
  PRIMARY KEY (`EMP_ID`)
);

CREATE TABLE `journal` (
  `ordering` bigint NOT NULL AUTO_INCREMENT,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  `persistence_id` varchar(255) NOT NULL,
  `sequence_number` bigint NOT NULL,
  `message` blob NOT NULL,
  `tags` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`persistence_id`,`sequence_number`),
  UNIQUE KEY `ordering` (`ordering`),
  UNIQUE KEY `journal_ordering_idx` (`ordering`)
);

CREATE TABLE `snapshot` (
  `persistence_id` varchar(255) NOT NULL,
  `sequence_number` bigint NOT NULL,
  `created` bigint NOT NULL,
  `snapshot` blob NOT NULL,
  PRIMARY KEY (`persistence_id`,`sequence_number`)
);

CREATE DATABASE intimations_passwordless_schema;

USE intimations_passwordless_schema;

CREATE TABLE `employees` (
  `ID` bigint NOT NULL,
  `NAME` text NOT NULL,
  `EMAIL` text NOT NULL,
  `ROLES` text NOT NULL,
  PRIMARY KEY (`ID`)
) ;

CREATE TABLE `otps` (
  `OTP` int NOT NULL,
  `EMP_ID` bigint NOT NULL,
  `EMAIL` varchar(254) NOT NULL,
  `ROLES` text NOT NULL,
  `CREATED_AT` text NOT NULL,
  PRIMARY KEY (`EMAIL`)
);

CREATE TABLE `refresh_tokens` (
  `REFRESH_TOKEN` text NOT NULL,
  `EMP_ID` bigint NOT NULL,
  `EMAIL` varchar(254) NOT NULL,
  `CREATED_AT` text NOT NULL,
  PRIMARY KEY (`EMAIL`)
);

CREATE TABLE `journal` (
  `ordering` bigint NOT NULL AUTO_INCREMENT,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  `persistence_id` varchar(255) NOT NULL,
  `sequence_number` bigint NOT NULL,
  `message` blob NOT NULL,
  `tags` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`persistence_id`,`sequence_number`),
  UNIQUE KEY `ordering` (`ordering`),
  UNIQUE KEY `journal_ordering_idx` (`ordering`)
);

CREATE TABLE `snapshot` (
  `persistence_id` varchar(255) NOT NULL,
  `sequence_number` bigint NOT NULL,
  `created` bigint NOT NULL,
  `snapshot` blob NOT NULL,
  PRIMARY KEY (`persistence_id`,`sequence_number`)
);
