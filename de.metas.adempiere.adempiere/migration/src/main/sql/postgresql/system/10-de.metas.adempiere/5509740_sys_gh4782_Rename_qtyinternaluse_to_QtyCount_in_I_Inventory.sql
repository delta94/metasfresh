-- 2019-01-16T17:13:20.015
-- I forgot to set the DICTIONARY_ID_COMMENTS System Configurator
UPDATE AD_Column SET AD_Reference_ID=14, FieldLength=2000,Updated=TO_TIMESTAMP('2019-01-16 17:13:19','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Column_ID=561006
;

-- 2019-01-16T17:13:27.442
-- I forgot to set the DICTIONARY_ID_COMMENTS System Configurator
INSERT INTO t_alter_column values('m_product','Warehouse_temperature','VARCHAR(2000)',null,null)
;

-- 2019-01-17T15:40:06.498
-- I forgot to set the DICTIONARY_ID_COMMENTS System Configurator
UPDATE AD_Column SET AD_Element_ID=1049, ColumnName='QtyCount', Description='Gezählte Menge', Help='"Zählmenge" zeigt die tatsächlich ermittelte Menge für ein Produkt im Bestand an.', Name='Zählmenge',Updated=TO_TIMESTAMP('2019-01-17 15:40:06','YYYY-MM-DD HH24:MI:SS'),UpdatedBy=100 WHERE AD_Column_ID=8823
;

-- 2019-01-17T15:40:06.552
-- I forgot to set the DICTIONARY_ID_COMMENTS System Configurator
UPDATE AD_Field SET Name='Zählmenge', Description='Gezählte Menge', Help='"Zählmenge" zeigt die tatsächlich ermittelte Menge für ein Produkt im Bestand an.' WHERE AD_Column_ID=8823
;

alter table i_inventory rename column qtyinternaluse to QtyCount;

