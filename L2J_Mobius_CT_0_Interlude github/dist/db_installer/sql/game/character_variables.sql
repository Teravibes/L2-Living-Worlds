CREATE TABLE IF NOT EXISTS `character_variables` (
  `charId` int(10) UNSIGNED NOT NULL,
  `var` varchar(255) NOT NULL,
  `val` text NOT NULL
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE INDEX idx_charId ON character_variables (charId);
CREATE INDEX idx_var ON character_variables (var);
