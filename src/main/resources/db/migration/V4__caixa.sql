CREATE TABLE IF NOT EXISTS template_venda (
                                              id IDENTITY PRIMARY KEY,
                                              nome        VARCHAR(100) NOT NULL,
    preco       DECIMAL(10,2) NOT NULL
    );

CREATE TABLE IF NOT EXISTS venda (
                                     id IDENTITY PRIMARY KEY,
                                     data_hora   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     descricao   VARCHAR(200)   NOT NULL,
    valor_bruto DECIMAL(10,2)  NOT NULL,
    metodo      VARCHAR(20)    NOT NULL,  -- DINHEIRO | CARTAO | PIX
    taxa        DECIMAL(10,2)  NOT NULL,  -- taxa cobrada (ex.: cartão)
    valor_liq   DECIMAL(10,2)  NOT NULL   -- valor_bruto - taxa
    );
