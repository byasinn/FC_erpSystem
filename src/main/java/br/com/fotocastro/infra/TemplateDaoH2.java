package br.com.fotocastro.infra;

import br.com.fotocastro.model.TemplateVenda;
import br.com.fotocastro.template.TemplateIcon;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * DAO para templates de venda com suporte completo a novos campos.
 */
public class TemplateDaoH2 extends DAOBase {

    /**
     * Lista todos os templates ativos ordenados por nome
     */
    public List<TemplateVenda> listar() {
        String sql = "SELECT * FROM sale_templates WHERE active = TRUE ORDER BY name";
        return executeQuery(sql, this::mapToTemplate);
    }

    /**
     * Lista todos os templates (incluindo inativos)
     */
    public List<TemplateVenda> listarTodos() {
        String sql = "SELECT * FROM sale_templates ORDER BY name";
        return executeQuery(sql, this::mapToTemplate);
    }

    /**
     * Busca template por ID
     */
    public Optional<TemplateVenda> buscarPorId(Long id) {
        String sql = "SELECT * FROM sale_templates WHERE id = ?";
        return executeQuerySingle(sql, this::mapToTemplate, id);
    }

    /**
     * Busca template por nome (case insensitive)
     */
    public Optional<TemplateVenda> buscarPorNome(String nome) {
        String sql = "SELECT * FROM sale_templates WHERE LOWER(name) = LOWER(?)";
        return executeQuerySingle(sql, this::mapToTemplate, nome.trim());
    }

    /**
     * Insere um novo template
     */
    public Long inserir(TemplateVenda template) {
        validateTemplate(template);
        
        String sql = "INSERT INTO sale_templates (name, price, size, icon, tag, estoque_item_id, quantidade_uso, active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        Long id = executeInsert(sql,
            template.getNome().trim(),
            template.getPreco(),
            template.getTamanho(),
            template.getIcone() != null ? template.getIcone().name() : null,
            template.getTag(),
            template.getEstoqueItemId(),
            template.getQuantidadeUso(),
            template.isAtivo()
        );
        
        logger.info(String.format("Template criado: %s - R$ %.2f (ID: %d)", 
            template.getNome(), template.getPreco(), id));
        return id;
    }

    /**
     * Atualiza um template existente
     */
    public void atualizar(TemplateVenda template) {
        validateTemplate(template);
        
        if (template.getId() == null) {
            throw new IllegalArgumentException("ID do template não pode ser nulo");
        }
        
        String sql = "UPDATE sale_templates SET name = ?, price = ?, size = ?, icon = ?, tag = ?, " +
                     "estoque_item_id = ?, quantidade_uso = ?, active = ?, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE id = ?";
        
        int affected = executeUpdate(sql,
            template.getNome().trim(),
            template.getPreco(),
            template.getTamanho(),
            template.getIcone() != null ? template.getIcone().name() : null,
            template.getTag(),
            template.getEstoqueItemId(),
            template.getQuantidadeUso(),
            template.isAtivo(),
            template.getId()
        );
        
        if (affected == 0) {
            throw new DAOException("Template não encontrado para atualização: " + template.getId());
        }
        
        logger.info("Template atualizado: ID=" + template.getId());
    }

    /**
     * Remove um template (soft delete - marca como inativo)
     */
    public void desativar(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID não pode ser nulo");
        }
        
        String sql = "UPDATE sale_templates SET active = FALSE WHERE id = ?";
        
        int affected = executeUpdate(sql, id);
        
        if (affected == 0) {
            throw new DAOException("Template não encontrado: " + id);
        }
        
        logger.info("Template desativado: ID=" + id);
    }

    /**
     * Remove permanentemente um template
     */
    public void remover(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID não pode ser nulo");
        }
        
        String sql = "DELETE FROM sale_templates WHERE id = ?";
        
        int affected = executeUpdate(sql, id);
        
        if (affected == 0) {
            throw new DAOException("Template não encontrado para remoção: " + id);
        }
        
        logger.info("Template removido permanentemente: ID=" + id);
    }

    /**
     * Conta total de templates ativos
     */
    public int contar() {
        String sql = "SELECT COUNT(*) FROM sale_templates WHERE active = TRUE";
        return executeScalar(sql, Integer.class);
    }

    /**
     * Verifica se existe algum template cadastrado
     */
    public boolean existeAlgum() {
        return contar() > 0;
    }

    /**
     * Lista templates por tag
     */
    public List<TemplateVenda> listarPorTag(String tag) {
        String sql = "SELECT * FROM sale_templates WHERE active = TRUE AND LOWER(tag) = LOWER(?) ORDER BY name";
        return executeQuery(sql, this::mapToTemplate, tag.trim());
    }

    /**
     * Lista templates com estoque vinculado
     */
    public List<TemplateVenda> listarComEstoque() {
        String sql = "SELECT * FROM sale_templates WHERE active = TRUE AND estoque_item_id IS NOT NULL ORDER BY name";
        return executeQuery(sql, this::mapToTemplate);
    }

    /**
     * Busca template com informações de estoque preenchidas
     */
    public Optional<TemplateVenda> buscarComEstoque(Long id) {
        String sql = 
            "SELECT t.*, e.nome AS estoque_nome, e.quantidade AS estoque_qtd " +
            "FROM sale_templates t " +
            "LEFT JOIN estoque e ON e.id = t.estoque_item_id " +
            "WHERE t.id = ?";
        
        return executeQuerySingle(sql, this::mapToTemplateComEstoque, id);
    }

    /**
     * Lista todos os templates com informações de estoque
     */
    public List<TemplateVenda> listarComEstoqueInfo() {
        String sql = 
            "SELECT t.*, e.nome AS estoque_nome, e.quantidade AS estoque_qtd " +
            "FROM sale_templates t " +
            "LEFT JOIN estoque e ON e.id = t.estoque_item_id " +
            "WHERE t.active = TRUE " +
            "ORDER BY t.name";
        
        return executeQuery(sql, this::mapToTemplateComEstoque);
    }

    // ========== MAPPERS ==========

    /**
     * Mapeia ResultSet para TemplateVenda
     */
    private TemplateVenda mapToTemplate(ResultSet rs) throws SQLException {
        TemplateVenda t = new TemplateVenda();
        t.setId(rs.getLong("id"));
        t.setNome(rs.getString("name"));
        t.setPreco(rs.getDouble("price"));
        
        String size = rs.getString("size");
        if (size != null && !size.trim().isEmpty()) {
            t.setTamanho(size);
        }
        
        String iconStr = rs.getString("icon");
        if (iconStr != null && !iconStr.trim().isEmpty()) {
            try {
                t.setIcone(TemplateIcon.valueOf(iconStr));
            } catch (IllegalArgumentException e) {
                logger.warning("Ícone inválido no template ID " + t.getId() + ": " + iconStr);
                t.setIcone(TemplateIcon.PHOTO); // padrão
            }
        }
        
        String tag = rs.getString("tag");
        if (tag != null && !tag.trim().isEmpty()) {
            t.setTag(tag);
        }
        
        Long estoqueId = rs.getObject("estoque_item_id", Long.class);
        if (estoqueId != null) {
            t.setEstoqueItemId(estoqueId);
        }
        
        t.setQuantidadeUso(rs.getInt("quantidade_uso"));
        t.setAtivo(rs.getBoolean("active"));
        
        return t;
    }

    /**
     * Mapeia ResultSet com informações de estoque
     */
    private TemplateVenda mapToTemplateComEstoque(ResultSet rs) throws SQLException {
        TemplateVenda t = mapToTemplate(rs);
        
        // Adiciona informações de estoque se existir
        try {
            String estoqueNome = rs.getString("estoque_nome");
            if (estoqueNome != null) {
                t.setEstoqueItemNome(estoqueNome);
            }
            
            int estoqueQtd = rs.getInt("estoque_qtd");
            if (!rs.wasNull()) {
                t.setEstoqueDisponivel(estoqueQtd);
            }
        } catch (SQLException ignored) {
            // Colunas não existem no ResultSet, ok
        }
        
        return t;
    }

    /**
     * Valida campos obrigatórios do template
     */
    private void validateTemplate(TemplateVenda template) {
        if (template == null) {
            throw new IllegalArgumentException("Template não pode ser nulo");
        }
        
        if (template.getNome() == null || template.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("Nome do template é obrigatório");
        }
        
        if (template.getPreco() < 0) {
            throw new IllegalArgumentException("Preço não pode ser negativo");
        }
        
        if (template.getQuantidadeUso() < 1) {
            throw new IllegalArgumentException("Quantidade de uso deve ser pelo menos 1");
        }
    }
}