CREATE TABLE IF NOT EXISTS `mods_wedding` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `player1Id` int(11) NOT NULL DEFAULT '0',
  `player2Id` int(11) NOT NULL DEFAULT '0',
  `married` varchar(5) DEFAULT NULL,
  `affianceDate` decimal(20,0) DEFAULT '0',
  `weddingDate` decimal(20,0) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `player1Id` (`player1Id`),
  KEY `player2Id` (`player2Id`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;