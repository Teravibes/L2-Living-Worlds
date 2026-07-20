CREATE TABLE IF NOT EXISTS `lottery` (
  `id` INT NOT NULL DEFAULT 0,
  `idnr` INT NOT NULL DEFAULT 0,
  `number1` INT NOT NULL DEFAULT 0,
  `number2` INT NOT NULL DEFAULT 0,
  `prize` INT NOT NULL DEFAULT 0,
  `newprize` INT NOT NULL DEFAULT 0,
  `prize1` INT NOT NULL DEFAULT 0,
  `prize2` INT NOT NULL DEFAULT 0,
  `prize3` INT NOT NULL DEFAULT 0,
  `enddate` bigint(13) unsigned NOT NULL DEFAULT '0',
  `finished` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`,`idnr`)
) DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;