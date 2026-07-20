CREATE TABLE IF NOT EXISTS `rainbowsprings_attacker_list` (
  `clanId` int(10) DEFAULT NULL,
  `war_decrees_count` double(20,0) DEFAULT NULL,
  KEY `clanid` (`clanid`)
) DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;