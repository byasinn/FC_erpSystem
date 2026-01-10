-- Adiciona campo para controlar último fechamento
ALTER TABLE fechamento_caixa ADD COLUMN IF NOT EXISTS automatico BOOLEAN DEFAULT FALSE;

-- Adiciona índice para melhorar performance
CREATE INDEX IF NOT EXISTS idx_venda_data ON venda(data_hora);
CREATE INDEX IF NOT EXISTS idx_fechamento_data ON fechamento_caixa(data);

-- Adiciona campo observacao em venda (se não existir)
ALTER TABLE venda ADD COLUMN IF NOT EXISTS observacao VARCHAR(500);