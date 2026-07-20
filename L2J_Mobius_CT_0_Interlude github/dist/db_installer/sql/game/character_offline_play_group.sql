DROP TABLE IF EXISTS character_offline_play_group;
CREATE TABLE IF NOT EXISTS character_offline_play_group (
  leaderId INT UNSIGNED NOT NULL DEFAULT 0,
  charId INT UNSIGNED NOT NULL DEFAULT 0,
  type TINYINT UNSIGNED NOT NULL DEFAULT 0,
  KEY leaderId (leaderId)
) DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;