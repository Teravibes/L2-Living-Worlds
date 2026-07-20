CREATE TABLE IF NOT EXISTS `merchant_lease` (
  `merchant_id` int(11) NOT NULL DEFAULT 0,
  `player_id` int(11) NOT NULL DEFAULT 0,
  `bid` int(11),
  `type` int(11) NOT NULL DEFAULT 0,
  `player_name` varchar(35),
  PRIMARY KEY (`merchant_id`,`player_id`,`type`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;