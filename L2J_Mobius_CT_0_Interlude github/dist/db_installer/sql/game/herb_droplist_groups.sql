-- This table holds data definitions for different herb dropping groups.
-- These groups once they're bound with mobs in the npc table, define which mobs
-- drop which herbs (with a certain chance to drop none, one or more of them).
--
-- If a mob appertain to group 0 (table default), it won't drop herbs at all.
-- For any other group larger than zero, herbs will be drop same way as droplists works
-- You can add any type of items here even not herbs, but players will auto loot it if
-- AUTO_LOOT_HERBS config is true.
--
-- About categries:
-- 0: is for Vitality herbs
-- 1: is for Life herbs
-- 2: is for Mana herbs
-- 3: is for special herbs
-- all other: is for Common herbs

DROP TABLE IF EXISTS `herb_droplist_groups`;
CREATE TABLE `herb_droplist_groups` (
  `groupId` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `itemId` smallint(5) unsigned NOT NULL DEFAULT '0',
  `min` smallint(2) unsigned NOT NULL DEFAULT '0',
  `max` smallint(2) unsigned NOT NULL DEFAULT '0',
  `category` smallint(3) NOT NULL DEFAULT '0',
  `chance` mediumint(7) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`groupId`,`itemId`,`category`),
  KEY `key_mobId` (`groupId`)
) DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;

INSERT INTO `herb_droplist_groups` VALUES
-- default group
(1,13028,1,1,0,20000), -- Vitality Replenishing Herb
(1,8600,1,1,1,100000), -- Herb of Life
(1,8601,1,1,1,40000), -- Greater Herb of Life
(1,8602,1,1,1,8000), -- Superior Herb of Life
(1,8603,1,1,2,100000), -- Herb of Mana
(1,8604,1,1,2,40000), -- Greater Herb of Mana
(1,8605,1,1,2,8000), -- Superior Herb of Mana
(1,8612,1,1,3,2000), -- Herb of the Warrior
(1,8613,1,1,3,2000), -- Herb of the Mystic
(1,8614,1,1,3,2000), -- Herb of Recovery
(1,8606,1,1,4,150000), -- Herb of Power
(1,8607,1,1,5,150000), -- Herb of Magic
(1,8608,1,1,6,150000), -- Herb of Atk. Spd.
(1,8609,1,1,7,150000), -- Herb of Casting Spd.
(1,8610,1,1,8,150000), -- Herb of Critical Attack - Probability
(1,8611,1,1,9,150000), -- Herb of Speed
(1,10655,1,1,10,150000), -- Herb of Vampiric Rage
(1,10656,1,1,11,150000), -- Herb of Critical Attack - Power
(1,10657,1,1,12,50000), -- Herb of Doubt
-- primeval island mobs. Guessed chances
(2,13028,1,1,0,20000), -- Vitality Replenishing Herb
(2,8600,1,1,1,200000), -- Herb of Life
(2,8601,1,1,1,80000), -- Greater Herb of Life
(2,8602,1,1,1,16000), -- Superior Herb of Life
(2,8603,1,1,2,150000), -- Herb of Mana
(2,8604,1,1,2,60000), -- Greater Herb of Mana
(2,8605,1,1,2,12000), -- Superior Herb of Mana
(2,8613,1,1,3,4000), -- Herb of the Mystic
(2,8614,1,1,3,4000), -- Herb of Recovery
(2,8607,1,1,4,150000), -- Herb of Magic
(2,8609,1,1,5,150000), -- Herb of Casting Spd.
(2,8611,1,1,6,150000), -- Herb of Speed
(2,10657,1,1,7,75000), -- Herb of Doubt
-- Field of Silence & Field of Whisper mobs
(3,13028,1,1,0,5000), -- Vitality Replenishing Herb
(3,8600,1,1,1,210000), -- Herb of Life
(3,8601,1,1,1,150000), -- Greater Herb of Life
(3,8602,1,1,1,35000), -- Superior Herb of Life
(3,8603,1,1,2,190000), -- Herb of Mana
(3,8604,1,1,2,180000), -- Greater Herb of Mana
(3,8605,1,1,2,50000), -- Superior Herb of Mana
(3,8612,1,1,3,2000), -- Herb of the Warrior
(3,8613,1,1,3,10000), -- Herb of the Mystic
(3,8614,1,1,3,2000), -- Herb of Recovery
(3,8606,1,1,4,60000), -- Herb of Power
(3,8607,1,1,5,60000), -- Herb of Magic
(3,8608,1,1,6,80000), -- Herb of Atk. Spd.
(3,8609,1,1,7,40000), -- Herb of Casting Spd.
(3,8610,1,1,8,20000), -- Herb of Critical Attack - Probability
(3,8611,1,1,9,90000), -- Herb of Speed
(3,10655,1,1,10,60000), -- Herb of Vampiric Rage
(3,10656,1,1,11,30000), -- Herb of Critical Attack - Power
(3,10657,1,1,12,5000), -- Herb of Doubt
(3,14824,1,1,13,9000),-- Ancient Herb - Slayer
(3,14825,1,1,13,9000),-- Ancient Herb - Immortal
(3,14826,1,1,13,25000), -- Ancient Herb - Terminator
(3,14827,1,1,13,30000), -- Ancient Herb - Guide
-- Sel Mahum training grounds - chief group
(4,13028,1,1,0,3299), -- Vitality Replenishing Herb
(4,8600,1,1,1,231000), -- Herb of Life
(4,8601,1,1,1,159600), -- Greater Herb of Life
(4,8602,1,1,1,29400), -- Superior Herb of Life
(4,8603,1,1,2,44000), -- Herb of Mana
(4,8604,1,1,2,57200), -- Greater Herb of Mana
(4,8605,1,1,2,8800), -- Superior Herb of Mana
(4,8612,1,1,3,3300), -- Herb of the Warrior
(4,8613,1,1,3,3300), -- Herb of the Mystic
(4,8614,1,1,3,3400), -- Herb of Recovery
(4,8606,1,1,4,50000), -- Herb of Power
(4,8608,1,1,4,50000), -- Herb of Atk. Spd.
(4,8610,1,1,4,50000), -- Herb of Critical Attack - Probability
(4,10655,1,1,4,50000), -- Herb of Vampiric Rage
(4,10656,1,1,4,50000), -- Herb of Critical Attack - Power
(4,8607,1,1,5,50000), -- Herb of Magic
(4,8609,1,1,5,50000), -- Herb of Casting Spd.
(4,8611,1,1,6,103400), -- Herb of Speed
(4,10657,1,1,6,3299), -- Herb of Doubt
-- Sel Mahum training grounds - soldier group
(5,8600,1,1,1,275000), -- Herb of Life
(5,8601,1,1,1,190000), -- Greater Herb of Life
(5,8602,1,1,1,35000), -- Superior Herb of Life
-- Sel Mahum training grounds - squad leader group
(6,13028,1,1,0,3300), -- Vitality Replenishing Herb
(6,8600,1,1,1,231000), -- Herb of Life
(6,8601,1,1,1,159600), -- Greater Herb of Life
(6,8602,1,1,1,29400), -- Superior Herb of Life
(6,8603,1,1,2,44000), -- Herb of Mana
(6,8604,1,1,2,57200), -- Greater Herb of Mana
(6,8605,1,1,2,8800), -- Superior Herb of Mana
(6,8606,1,1,3,50000), -- Herb of Power
(6,8608,1,1,3,50000), -- Herb of Atk. Spd.
(6,8610,1,1,3,50000), -- Herb of Critical Attack - Probability
(6,10655,1,1,3,50000), -- Herb of Vampiric Rage
(6,10656,1,1,3,50000), -- Herb of Critical Attack - Power
(6,8607,1,1,4,50000), -- Herb of Magic
(6,8609,1,1,4,50000), -- Herb of Casting Spd.
(6,8612,1,1,5,3300), -- Herb of the Warrior
(6,8613,1,1,5,3300), -- Herb of the Mystic
(6,8614,1,1,5,3400), -- Herb of Recovery
(6,8611,1,1,6,103400), -- Herb of Speed
(6,10657,1,1,6,3300), -- Herb of Doubt
(6,10655,1,1,7,450000), -- Herb of Vampiric Rage
-- Antharas minions - Behemoth and Tarask Dragon
(7,8952,10,20,1,1000000), -- Greater Herb of Life
(7,8953,10,20,2,1000000), -- Greater Herb of Mana
-- Antharas minions - Dragon Bomber
(8,8952,10,20,1,1000000), -- Greater Herb of Life
(8,8953,10,20,2,1000000); -- Greater Herb of Mana
