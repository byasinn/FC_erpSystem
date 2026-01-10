CREATE TABLE estoque (
                         id IDENTITY PRIMARY KEY,
                         nome VARCHAR(100) NOT NULL,
                         cor VARCHAR(50),
                         quantidade INT NOT NULL DEFAULT 0,
                         custo_total DECIMAL(10,2) NOT NULL DEFAULT 0.00
);
