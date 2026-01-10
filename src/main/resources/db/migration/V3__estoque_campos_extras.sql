ALTER TABLE estoque ADD COLUMN tamanho VARCHAR(50);
-- se já existir, ignore o erro no Flyway com clean/re-run só se precisar.
