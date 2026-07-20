DROP TABLE IF EXISTS character_offline_play;
CREATE TABLE IF NOT EXISTS character_offline_play (
  charId INT UNSIGNED NOT NULL DEFAULT 0,
  type TINYINT UNSIGNED NOT NULL DEFAULT 0,
  id INT NOT NULL DEFAULT 0,
  KEY charId (charId)
) DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;