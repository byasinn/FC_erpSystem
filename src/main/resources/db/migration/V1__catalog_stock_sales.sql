-- Tipos básicos e tabelas do módulo de catálogo/estoque/caixa
-- V2: catálogo + estoque + vendas + pagamentos + saídas avulsas

-- Itens do catálogo (MATERIAL, PRODUCT, SERVICE)
CREATE TABLE items (
                       id            IDENTITY PRIMARY KEY,
                       name          VARCHAR(120) NOT NULL,
                       sku           VARCHAR(60),
                       kind          VARCHAR(20)  NOT NULL,  -- 'MATERIAL' | 'PRODUCT' | 'SERVICE'
                       size          VARCHAR(40),
                       sell_price    DECIMAL(10,2),
                       track_stock   BOOLEAN DEFAULT TRUE,
                       CONSTRAINT uq_items_sku UNIQUE (sku),
                       CONSTRAINT ck_items_kind CHECK (kind IN ('MATERIAL','PRODUCT','SERVICE'))
);

-- Estoque agregado (quantidade atual + custo médio)
CREATE TABLE inventory (
                           item_id    BIGINT PRIMARY KEY,
                           quantity   DECIMAL(12,3) NOT NULL DEFAULT 0,
                           avg_cost   DECIMAL(10,4) NOT NULL DEFAULT 0,
                           CONSTRAINT fk_inventory_item FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
);

-- Movimentações de estoque (histórico)
CREATE TABLE stock_movements (
                                 id          IDENTITY PRIMARY KEY,
                                 item_id     BIGINT NOT NULL,
                                 mtype       VARCHAR(10) NOT NULL,    -- 'IN' | 'OUT' | 'ADJUST'
                                 quantity    DECIMAL(12,3) NOT NULL,
                                 unit_cost   DECIMAL(10,4) NOT NULL,
                                 total_cost  DECIMAL(12,4) NOT NULL,
                                 reason      VARCHAR(120),
                                 ref_sale_id BIGINT,
                                 created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                 user_id     BIGINT,
                                 CONSTRAINT fk_sm_item FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE,
                                 CONSTRAINT ck_sm_mtype CHECK (mtype IN ('IN','OUT','ADJUST'))
);
CREATE INDEX ix_sm_item ON stock_movements(item_id);
CREATE INDEX ix_sm_created_at ON stock_movements(created_at);

-- BOM: componentes consumidos por um item vendido (ex.: SERVIÇO -> MATERIAL)
CREATE TABLE bom (
                     parent_item_id    BIGINT NOT NULL,  -- item vendido (PRODUCT/SERVICE)
                     component_item_id BIGINT NOT NULL,  -- item de estoque consumido (MATERIAL/PRODUCT com track)
                     qty               DECIMAL(12,3) NOT NULL,
                     PRIMARY KEY (parent_item_id, component_item_id),
                     CONSTRAINT fk_bom_parent FOREIGN KEY (parent_item_id) REFERENCES items(id) ON DELETE CASCADE,
                     CONSTRAINT fk_bom_comp   FOREIGN KEY (component_item_id) REFERENCES items(id) ON DELETE CASCADE
);

-- Vendas
CREATE TABLE sales (
                       id           IDENTITY PRIMARY KEY,
                       created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       total_gross  DECIMAL(12,2) NOT NULL,
                       discount     DECIMAL(12,2) NOT NULL DEFAULT 0,
                       total_net    DECIMAL(12,2) NOT NULL,
                       user_id      BIGINT
);
CREATE INDEX ix_sales_created_at ON sales(created_at);

-- Itens da venda (snapshot de nome e preço)
CREATE TABLE sale_items (
                            id          IDENTITY PRIMARY KEY,
                            sale_id     BIGINT NOT NULL,
                            item_id     BIGINT,                -- pode ficar null se item do catálogo for removido depois
                            name_snap   VARCHAR(120) NOT NULL,
                            qty         DECIMAL(12,3) NOT NULL,
                            unit_price  DECIMAL(10,2) NOT NULL,
                            total_price DECIMAL(12,2) NOT NULL,
                            CONSTRAINT fk_si_sale FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE
);
CREATE INDEX ix_si_sale ON sale_items(sale_id);

-- Pagamentos (suporta split por método)
CREATE TABLE payments (
                          id       IDENTITY PRIMARY KEY,
                          sale_id  BIGINT NOT NULL,
                          method   VARCHAR(20) NOT NULL,   -- 'CASH'|'PIX'|'DEBIT'|'CREDIT'|'OTHER'
                          amount   DECIMAL(12,2) NOT NULL,
                          CONSTRAINT fk_pay_sale FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE,
                          CONSTRAINT ck_pay_method CHECK (method IN ('CASH','PIX','DEBIT','CREDIT','OTHER'))
);
CREATE INDEX ix_pay_sale ON payments(sale_id);

-- Saídas avulsas de caixa (não vinculadas a venda)
CREATE TABLE cash_outflows (
                               id         IDENTITY PRIMARY KEY,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               method     VARCHAR(20) NOT NULL,  -- 'CASH'|'PIX'|'DEBIT'|'CREDIT'|'OTHER'
                               amount     DECIMAL(12,2) NOT NULL,
                               note       VARCHAR(140),
                               user_id    BIGINT,
                               CONSTRAINT ck_out_method CHECK (method IN ('CASH','PIX','DEBIT','CREDIT','OTHER'))
);
CREATE INDEX ix_out_created_at ON cash_outflows(created_at);
