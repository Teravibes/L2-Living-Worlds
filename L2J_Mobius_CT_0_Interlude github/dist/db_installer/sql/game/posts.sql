CREATE TABLE IF NOT EXISTS `posts` (
  `post_id` int(8) NOT NULL DEFAULT '0',
  `post_owner_name` varchar(255) NOT NULL DEFAULT '',
  `post_ownerid` int(8) NOT NULL DEFAULT '0',
  `post_date` bigint(13) unsigned NOT NULL DEFAULT '0',
  `post_topic_id` int(8) NOT NULL DEFAULT '0',
  `post_forum_id` int(8) NOT NULL DEFAULT '0',
  `post_txt` text NOT NULL,
  KEY `post_forum_id` (`post_forum_id`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;