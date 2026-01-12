# FotoCastro ERP

Sistema de gestão para fotocentro / estúdio fotográfico: templates de venda, controle de estoque, caixa, fechamentos e dashboard básico.

**Status:** Em desenvolvimento – v0.1.1 (alpha)

### Changelog

**v0.1.1**
- Home reestruturada com TabPane: abas "Caixas Anteriores", "Relatórios", "Avisos" e "Mais"
- Aba "Caixas Anteriores": listagem real de fechamentos da tabela `fechamento_caixa` (DAO atualizado com `listarFechamentos`)
- Detalhes de fechamento em Alert com bruto, líquido, pagamentos, diferença, observação e tipo (auto/manual)
- Exportação PDF básica de fechamento selecionado (iText 7) – salva no Desktop por enquanto
- Correções gerais: remoção de classes duplicadas (FechamentoResumo), imports faltantes, colunas locais no TableView
- Placeholders mantidos para avisos e relatórios (próximos passos)

**v0.1.0**
- Estoque 100%: cadastro, edição, movimentação, histórico
- Caixa ~95%: vendas, pagamentos, taxas, fechamento parcial
- Dashboard ~75%: KPIs, gráficos de vendas diárias/horárias, top templates
- Templates avançados: ícones, tags, vinculação estoque, consumo automático
- Banco H2 + Flyway (migrações V1-V8)
- DAO genérico + event bus para vendas
