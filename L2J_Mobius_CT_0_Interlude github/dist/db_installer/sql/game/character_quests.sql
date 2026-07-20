CREATE TABLE IF NOT EXISTS `character_quests` (
  `charId` INT UNSIGNED NOT NULL DEFAULT 0,
  `name` VARCHAR(60) NOT NULL DEFAULT '',
  `var`  VARCHAR(20) NOT NULL DEFAULT '',
  `value` VARCHAR(255) ,
  PRIMARY KEY (`charId`,`name`,`var`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

CREATE INDEX idx_charId_name ON character_quests (charId, name);
CREATE INDEX idx_charId_var ON character_quests (charId, var);
CREATE UNIQUE INDEX idx_charId_name_var ON character_quests (charId, name, var);
