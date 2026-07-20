CREATE TABLE IF NOT EXISTS `character_skills` (
  `charId` INT UNSIGNED NOT NULL DEFAULT 0,
  `skill_id` INT NOT NULL DEFAULT 0,
  `skill_level` INT(3) NOT NULL DEFAULT 1,
  `class_index` INT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`charId`,`skill_id`,`class_index`)
) DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;

# RESTORE_SKILLS_FOR_CHAR, DELETE_CHAR_SKILLS
CREATE INDEX idx_charId_classIndex ON character_skills (charId, class_index);

# UPDATE_CHARACTER_SKILL_LEVEL, ADD_NEW_SKILLS, DELETE_SKILL_FROM_CHAR
CREATE INDEX idx_skillId_charId_classIndex ON character_skills (skill_id, charId, class_index);
