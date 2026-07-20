CREATE TABLE IF NOT EXISTS `seven_signs_festival` (
  `festivalId` int(1) NOT NULL DEFAULT '0',
  `cabal` varchar(4) NOT NULL DEFAULT '',
  `cycle` int(4) NOT NULL DEFAULT '0',
  `date` bigint(13) unsigned NOT NULL DEFAULT '0',
  `score` int(5) NOT NULL DEFAULT '0',
  `members` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`festivalId`,`cabal`,`cycle`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

INSERT IGNORE INTO `seven_signs_festival` VALUES
(0, "dawn", 1, 0, 0, ""),
(1, "dawn", 1, 0, 0, ""),
(2, "dawn", 1, 0, 0, ""),
(3, "dawn", 1, 0, 0, ""),
(4, "dawn", 1, 0, 0, ""),
(0, "dusk", 1, 0, 0, ""),
(1, "dusk", 1, 0, 0, ""),
(2, "dusk", 1, 0, 0, ""),
(3, "dusk", 1, 0, 0, ""),
(4, "dusk", 1, 0, 0, "");