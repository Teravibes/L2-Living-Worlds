CREATE TABLE IF NOT EXISTS `auction` (
  `id` int(11) NOT NULL DEFAULT '0',
  `sellerId` int(11) NOT NULL DEFAULT '0',
  `sellerName` varchar(50) NOT NULL DEFAULT 'NPC',
  `sellerClanName` varchar(50) NOT NULL DEFAULT '',
  `itemType` varchar(25) NOT NULL DEFAULT '',
  `itemId` int(11) NOT NULL DEFAULT '0',
  `itemObjectId` int(11) NOT NULL DEFAULT '0',
  `itemName` varchar(40) NOT NULL DEFAULT '',
  `itemQuantity` BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `startingBid` BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `currentBid` BIGINT UNSIGNED NOT NULL DEFAULT 0,
  `endDate` bigint(13) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`itemType`,`itemId`,`itemObjectId`),
  KEY `id` (`id`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;