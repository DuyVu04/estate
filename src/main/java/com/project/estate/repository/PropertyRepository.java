package com.project.estate.repository;

import com.project.estate.entity.Property;
import com.project.estate.enums.PropertyStatus;
import com.project.estate.repository.projection.PropertySemanticProjection;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface PropertyRepository
    extends JpaRepository<Property, String>, JpaSpecificationExecutor<Property> {

  long countByStatus(PropertyStatus status);

  @Modifying
  @Transactional
  @Query(
      value = "UPDATE properties SET embedding = CAST(:vectorStr AS vector) WHERE id = :id",
      nativeQuery = true)
  int updateEmbedding(@Param("id") String id, @Param("vectorStr") String vectorStr);

  /** Pure Semantic Search using Cosine Distance (<=>) with HNSW Index. */
  @Query(
      value =
          """
          SELECT p.id AS id, p.title AS title, p.description AS description,
                 p.property_type AS propertyType, p.address AS address, p.ward AS ward,
                 p.district AS district, p.city AS city, p.area AS area, p.price AS price,
                 p.status AS status, p.created_at AS createdAt, p.updated_at AS updatedAt,
                 (1.0 - (p.embedding <=> CAST(:queryVector AS vector))) AS similarityScore
          FROM properties p
          WHERE p.status = 'AVAILABLE'
            AND p.embedding IS NOT NULL
          ORDER BY p.embedding <=> CAST(:queryVector AS vector)
          LIMIT :limit
          """,
      nativeQuery = true)
  List<PropertySemanticProjection> searchSemantic(
      @Param("queryVector") String queryVector, @Param("limit") int limit);

  /** Hybrid Search: Combines SQL Filters (City, Price) with Vector Cosine Distance. */
  @Query(
      value =
          """
          SELECT p.id AS id, p.title AS title, p.description AS description,
                 p.property_type AS propertyType, p.address AS address, p.ward AS ward,
                 p.district AS district, p.city AS city, p.area AS area, p.price AS price,
                 p.status AS status, p.created_at AS createdAt, p.updated_at AS updatedAt,
                 (1.0 - (p.embedding <=> CAST(:queryVector AS vector))) AS similarityScore
          FROM properties p
          WHERE p.status = 'AVAILABLE'
            AND (:city IS NULL OR p.city = :city)
            AND (:minPrice IS NULL OR p.price >= :minPrice)
            AND (:maxPrice IS NULL OR p.price <= :maxPrice)
            AND p.embedding IS NOT NULL
          ORDER BY p.embedding <=> CAST(:queryVector AS vector)
          LIMIT :limit
          """,
      nativeQuery = true)
  List<PropertySemanticProjection> searchHybrid(
      @Param("queryVector") String queryVector,
      @Param("city") String city,
      @Param("minPrice") BigDecimal minPrice,
      @Param("maxPrice") BigDecimal maxPrice,
      @Param("limit") int limit);
}
