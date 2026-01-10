ALTER TABLE fechamento_caixa
ADD COLUMN valor_contado DECIMAL(12,2);

ALTER TABLE fechamento_caixa
ADD COLUMN diferenca DECIMAL(12,2);

ALTER TABLE fechamento_caixa
ADD COLUMN observacao VARCHAR(255);