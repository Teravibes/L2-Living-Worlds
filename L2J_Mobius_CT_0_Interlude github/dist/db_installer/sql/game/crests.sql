CREATE TABLE IF NOT EXISTS `crests` (
	`crest_id` INT,
	`data` VARBINARY(2176) NOT NULL,
	`type` TINYINT NOT NULL,
	PRIMARY KEY(`crest_id`)
) DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;