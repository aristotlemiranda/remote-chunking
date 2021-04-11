-- scaling.`TRANSACTION` definition

CREATE TABLE `TRANSACTION` (
  `account` varchar(255) NOT NULL,
  `amount` decimal(19,2) DEFAULT NULL,
  `timestamp` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`account`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;