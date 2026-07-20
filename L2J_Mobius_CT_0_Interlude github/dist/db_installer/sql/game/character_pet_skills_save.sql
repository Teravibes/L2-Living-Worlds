CREATE TABLE IF NOT EXISTS `character_pet_skills_save` (
  `petObjItemId` INT NOT NULL DEFAULT 0,
  `skill_id` INT NOT NULL DEFAULT 0,
  `skill_level` INT(3) NOT NULL DEFAULT 1,
  `remaining_time` INT NOT NULL DEFAULT 0,
  `buff_index` INT(2) NOT NULL DEFAULT 0,
  PRIMARY KEY (`petObjItemId`,`skill_id`,`skill_level`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;