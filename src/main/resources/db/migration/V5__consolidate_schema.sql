-- ========================================
-- V5: Consolidação do Schema + Templates Avançados
-- ========================================

-- 1. Sistema de Configurações
CREATE TABLE IF NOT EXISTS system_config (
    config_key   VARCHAR(60) PRIMARY KEY,
    config_value VARCHAR(500),
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Configurações padrão de taxas
INSERT INTO system_config (config_key, config_value) VALUES 
    ('TAXA_CARTAO_PERCENT', '0.0533333333'),
    ('TAXA_PIX_PERCENT', '0.0'),
    ('TAXA_DINHEIRO_PERCENT', '0.0'),
    ('ESTOQUE_MINIMO_ALERTA', '10');

-- 2. Templates de Venda AVANÇADOS
CREATE TABLE IF NOT EXISTS sale_templates (
    id              IDENTITY PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    price           DECIMAL(10,2) NOT NULL,
    size            VARCHAR(40),           -- tamanho (ex: "10x15", "A4")
    icon            VARCHAR(20),           -- ícone (PHOTO, FRAME, DOCUMENT, POLAROID)
    tag             VARCHAR(60),           -- tag/categoria
    estoque_item_id BIGINT,                -- FK para estoque (opcional)
    quantidade_uso  INT DEFAULT 1,         -- quantas unidades de estoque consome
    active          BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_template_estoque FOREIGN KEY (estoque_item_id) REFERENCES estoque(id) ON DELETE SET NULL
);

-- Migrar templates antigos para nova estrutura (se existir)
INSERT INTO sale_templates (name, price, icon, tag)
SELECT nome, preco, 'PHOTO', 'Revelação' 
FROM template_venda 
WHERE EXISTS (SELECT 1 FROM template_venda LIMIT 1)
  AND NOT EXISTS (SELECT 1 FROM sale_templates WHERE name = template_venda.nome);

-- 3. Migrar dados de estoque para items + inventory
INSERT INTO items (name, sku, kind, size, sell_price, track_stock)
SELECT 
    nome,
    CONCAT('EST-', CAST(id AS VARCHAR)),
    'MATERIAL',
    tamanho,
    NULL,
    TRUE
FROM estoque
WHERE EXISTS (SELECT 1 FROM estoque LIMIT 1)
  AND NOT EXISTS (SELECT 1 FROM items WHERE sku = CONCAT('EST-', CAST(estoque.id AS VARCHAR)));

INSERT INTO inventory (item_id, quantity, avg_cost)
SELECT 
    i.id,
    e.quantidade,
    CASE WHEN e.quantidade > 0 THEN e.custo_total / e.quantidade ELSE 0 END
FROM estoque e
INNER JOIN items i ON i.sku = CONCAT('EST-', CAST(e.id AS VARCHAR))
WHERE EXISTS (SELECT 1 FROM estoque LIMIT 1)
  AND NOT EXISTS (SELECT 1 FROM inventory WHERE item_id = i.id);

-- 4. Índices para performance
CREATE INDEX IF NOT EXISTS ix_items_kind ON items(kind);
CREATE INDEX IF NOT EXISTS ix_items_track_stock ON items(track_stock);
CREATE INDEX IF NOT EXISTS ix_inventory_quantity ON inventory(quantity);
CREATE INDEX IF NOT EXISTS ix_sale_templates_active ON sale_templates(active);
CREATE INDEX IF NOT EXISTS ix_sale_templates_tag ON sale_templates(tag);

-- 5. Coluna observação em venda
ALTER TABLE venda ADD COLUMN IF NOT EXISTS observacao VARCHAR(200);

-- 6. Log de operações (auditoria)
CREATE TABLE IF NOT EXISTS operation_log (
    id           IDENTITY PRIMARY KEY,
    operation    VARCHAR(50) NOT NULL,
    entity_type  VARCHAR(30),
    entity_id    BIGINT,
    description  VARCHAR(200),
    user_name    VARCHAR(60),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ix_log_created_at ON operation_log(created_at);
CREATE INDEX IF NOT EXISTS ix_log_operation ON operation_log(operation);

-- 7. Views úteis
CREATE OR REPLACE VIEW vw_estoque_alerta AS
SELECT 
    i.id,
    i.name AS nome,
    i.size AS tamanho,
    inv.quantity AS quantidade,
    inv.avg_cost AS custo_medio,
    (inv.quantity * inv.avg_cost) AS valor_total,
    CASE 
        WHEN inv.quantity <= 10 THEN 'CRÍTICO'
        WHEN inv.quantity <= 20 THEN 'BAIXO'
        ELSE 'OK'
    END AS status
FROM items i
INNER JOIN inventory inv ON inv.item_id = i.id
WHERE i.track_stock = TRUE
ORDER BY inv.quantity ASC, i.name;

CREATE OR REPLACE VIEW vw_dashboard_vendas AS
SELECT 
    COUNT(*) AS total_vendas,
    COALESCE(SUM(valor_bruto), 0) AS total_bruto,
    COALESCE(SUM(taxa), 0) AS total_taxas,
    COALESCE(SUM(valor_liq), 0) AS total_liquido,
    COALESCE(SUM(CASE WHEN metodo = 'DINHEIRO' THEN valor_liq ELSE 0 END), 0) AS liquido_dinheiro,
    COALESCE(SUM(CASE WHEN metodo = 'CARTAO' THEN valor_liq ELSE 0 END), 0) AS liquido_cartao,
    COALESCE(SUM(CASE WHEN metodo = 'PIX' THEN valor_liq ELSE 0 END), 0) AS liquido_pix
FROM venda;

CREATE OR REPLACE VIEW vw_vendas_hoje AS
SELECT 
    COUNT(*) AS quantidade,
    COALESCE(SUM(valor_bruto), 0) AS total_bruto,
    COALESCE(SUM(valor_liq), 0) AS total_liquido
FROM venda
WHERE CAST(data_hora AS DATE) = CURRENT_DATE;

-- 8. Comentários nas tabelas
COMMENT ON TABLE sale_templates IS 'Templates de venda com ícones, tags e vinculação a estoque';

-- Trigger comentado (estava dando erro)
-- CREATE TRIGGER IF NOT EXISTS trg_sale_templates_updated 
-- BEFORE UPDATE ON sale_templates
-- FOR EACH ROW 
-- CALL 'br.com.fotocastro.infra.UpdateTimestampTrigger';

-- ========================================
-- FIM DA MIGRATION V5
-- Usuário criará seus próprios templates
-- ========================================