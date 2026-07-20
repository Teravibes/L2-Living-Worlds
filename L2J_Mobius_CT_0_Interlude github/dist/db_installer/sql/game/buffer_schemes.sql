CREATE TABLE IF NOT EXISTS `buffer_schemes` (
  `object_id` INT UNSIGNED NOT NULL DEFAULT '0',
  `scheme_name` VARCHAR(16) NOT NULL DEFAULT 'default',
  `skills` VARCHAR(512) NOT NULL,
  PRIMARY KEY (`object_id`,`scheme_name`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;