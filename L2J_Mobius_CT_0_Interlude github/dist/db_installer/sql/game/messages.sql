CREATE TABLE IF NOT EXISTS `messages` (
  `messageId` INT NOT NULL DEFAULT 0,
  `senderId` INT NOT NULL DEFAULT 0,
  `receiverId` INT NOT NULL DEFAULT 0,
  `subject` TINYTEXT,
  `content` TEXT,
  `expiration` bigint(13) unsigned NOT NULL DEFAULT '0',
  `reqAdena` BIGINT NOT NULL DEFAULT 0,
  `hasAttachments` enum('true','false') DEFAULT 'false' NOT NULL,
  `isUnread` enum('true','false') DEFAULT 'true' NOT NULL,
  `isDeletedBySender` enum('true','false') DEFAULT 'false' NOT NULL,
  `isDeletedByReceiver` enum('true','false') DEFAULT 'false' NOT NULL,
  `isLocked` enum('true','false') DEFAULT 'false' NOT NULL,
  `sendBySystem` tinyint(1) NOT NULL DEFAULT 0,
  `isReturned` enum('true','false') DEFAULT 'false' NOT NULL,
  PRIMARY KEY (`messageId`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;