CREATE TABLE IF NOT EXISTS `seven_signs` (
  `charId` INT UNSIGNED NOT NULL DEFAULT 0,
  `cabal` VARCHAR(4) NOT NULL DEFAULT '',
  `seal` INT(1) NOT NULL DEFAULT 0,
  `red_stones` INT NOT NULL DEFAULT 0,
  `green_stones` INT NOT NULL DEFAULT 0,
  `blue_stones` INT NOT NULL DEFAULT 0,
  `ancient_adena_amount` DECIMAL(20,0) NOT NULL DEFAULT 0,
  `contribution_score` DECIMAL(20,0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`charId`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;