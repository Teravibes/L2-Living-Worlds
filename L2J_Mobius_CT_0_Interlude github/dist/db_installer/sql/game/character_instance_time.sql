CREATE TABLE IF NOT EXISTS `character_instance_time` (
  `charId` INT UNSIGNED NOT NULL DEFAULT '0',
  `instanceId` int(3) NOT NULL DEFAULT '0',
  `time` bigint(13) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`charId`,`instanceId`)
) DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;