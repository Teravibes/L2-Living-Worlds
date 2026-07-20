CREATE TABLE IF NOT EXISTS `character_subclasses` (
  `charId` INT UNSIGNED NOT NULL DEFAULT 0,
  `class_id` int(2) NOT NULL DEFAULT 0,
  `exp` bigint(20) NOT NULL DEFAULT 0,
  `sp` bigint(10) NOT NULL DEFAULT 0,
  `level` int(2) NOT NULL DEFAULT 40,
  `class_index` int(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`charId`,`class_id`)
) DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;

# RESTORE_CHAR_SUBCLASSES, ADD_CHAR_SUBCLASS, UPDATE_CHAR_SUBCLASS, DELETE_CHAR_SUBCLASS
CREATE INDEX idx_charId_classIndex ON character_subclasses (charId, class_index);

# CharSelectionInfo
CREATE INDEX idx_charId_classId ON character_subclasses (charId, class_id);
