USE openiam_ce2;
ALTER TABLE `RECONCILIATION_CONFIG` 
ADD COLUMN `CSV_LINE_SEPARATOR` VARCHAR(10) NULL DEFAULT 'comma', 
ADD COLUMN `CSV_END_OF_LINE` VARCHAR(10) NULL DEFAULT 'enter';

