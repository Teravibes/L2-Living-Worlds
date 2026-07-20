CREATE TABLE IF NOT EXISTS `item_attributes` (
  `itemId` int(11) NOT NULL DEFAULT 0,
  `augAttributes` int(11) NOT NULL DEFAULT -1,
  PRIMARY KEY (`itemId`)
) DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;

CREATE INDEX idx_itemId ON item_attributes (itemId);
