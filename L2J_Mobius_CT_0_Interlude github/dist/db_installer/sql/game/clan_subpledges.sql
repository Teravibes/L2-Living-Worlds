CREATE TABLE IF NOT EXISTS `clan_subpledges` (
  `clan_id` INT NOT NULL DEFAULT '0',
  `sub_pledge_id` INT NOT NULL DEFAULT '0',
  `name` varchar(45),
  `leader_id` INT NOT NULL DEFAULT '0',
  PRIMARY KEY (`clan_id`,`sub_pledge_id`),
  KEY `leader_id` (`leader_id`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;