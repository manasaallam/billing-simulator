package com.example.billingsimulator.repository;

import com.example.billingsimulator.model.KnowledgeArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, UUID> {

    /**
     * Vector similarity search using pgvector cosine distance.
     * Returns top-K most relevant knowledge chunks for a given query embedding.
     *
     * @param queryVector embedding as string format '[0.1, 0.2, ...]'
     * @param limit       max results to return
     * @return rows of [article_id, content, kind, key_code, business_reason, similarity]
     */
    @Query(value = """
            SELECT article_id, content, kind, key_code, business_reason,
                   1 - (embedding <=> CAST(:queryVector AS vector)) AS similarity
            FROM knowledge_article
            WHERE company_id IS NULL
            ORDER BY embedding <=> CAST(:queryVector AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findSimilarGlobal(@Param("queryVector") String queryVector,
                                     @Param("limit") int limit);

    /**
     * Vector similarity search scoped to a specific company + global knowledge.
     */
    @Query(value = """
            SELECT article_id, content, kind, key_code, business_reason,
                   1 - (embedding <=> CAST(:queryVector AS vector)) AS similarity
            FROM knowledge_article
            WHERE company_id IS NULL OR company_id = CAST(:companyId AS uuid)
            ORDER BY embedding <=> CAST(:queryVector AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findSimilarForCompany(@Param("queryVector") String queryVector,
                                         @Param("companyId") String companyId,
                                         @Param("limit") int limit);

    Optional<KnowledgeArticle> findByCompanyIdAndKindAndKeyCode(UUID companyId, String kind, String keyCode);

    List<KnowledgeArticle> findByKind(String kind);

    long countByCompanyIdIsNull();

    @Modifying
    @Query("DELETE FROM KnowledgeArticle k WHERE k.companyId IS NULL")
    void deleteAllGlobal();
}
