CREATE TABLE IF NOT EXISTS `siegable_hall_flagwar_attackers_members` (
  `hall_id` tinyint(2) unsigned NOT NULL DEFAULT '0',
  `clan_id` int(10) unsigned NOT NULL DEFAULT '0',
  `object_id` int(10) unsigned NOT NULL DEFAULT '0',
  KEY `hall_id` (`hall_id`),
  KEY `clan_id` (`clan_id`),
  KEY `object_id` (`object_id`)
) DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;