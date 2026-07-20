-- schedule_config format: Time to add from the last siege in this format DD;MM;YY;HH;mm
-- DD = Days to add to the current date for next siege
-- MM = Month to add to the current date for the next siege
-- YY = Years to add to the current date for the next siege
-- HH = Hour of the day when siege must start
-- mm = Minutes of the day when siege must start
-- Example for a siege each 7 days which starts at 12 o clock: 7;0;0;12;00

CREATE TABLE IF NOT EXISTS `siegable_clanhall` (
  `clanHallId` int(10) NOT NULL DEFAULT '0',
  `name` varchar(45) DEFAULT NULL,
  `ownerId` int(10) DEFAULT NULL,
  `desc` varchar(100) DEFAULT NULL,
  `location` varchar(100) DEFAULT NULL,
  `nextSiege` bigint(20) DEFAULT NULL,
  `siegeLength` int(10) DEFAULT NULL,
  `schedule_config` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`clanHallId`),
  KEY `ownerId` (`ownerId`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

INSERT IGNORE INTO `siegable_clanhall` (`clanHallId`, `name`, `ownerId`, `desc`, `location`, `nextSiege`, `siegeLength`, `schedule_config`) VALUES
(21, 'Fortress of Resistance', 0, 'Contestable Clan Hall', 'Dion', 0, 3600000, '14;0;0;12;00'),
(34, 'Devastated Castle', 0, 'Contestable Clan Hall', 'Aden', 0, 3600000, '14;0;0;12;00'),
(35, 'Bandit StrongHold', 0, 'Contestable Clan Hall', 'Oren', 0, 3600000, '14;0;0;12;00'),
(62, 'Rainbow Springs', 0, 'Contestable Clan Hall', 'Goddard', 0, 3600000, '14;0;0;12;00'),
(63, 'Beast Farm', 0, 'Contestable Clan Hall', 'Rune', 0, 3600000, '14;0;0;12;00'),
(64, 'Fortress of the Dead', 0, 'Contestable Clan Hall', 'Rune', 0, 3600000, '14;0;0;12;00');