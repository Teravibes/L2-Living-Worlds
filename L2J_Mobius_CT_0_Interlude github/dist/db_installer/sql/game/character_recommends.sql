CREATE TABLE IF NOT EXISTS `character_recommends` (
  `charId` INT UNSIGNED NOT NULL default 0,
  `target_id` INT(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`charId`,`target_id`)
) DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;