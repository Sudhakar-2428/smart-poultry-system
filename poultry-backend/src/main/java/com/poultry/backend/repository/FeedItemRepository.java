package com.poultry.backend.repository;

import com.poultry.backend.entity.FeedItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedItemRepository extends JpaRepository<FeedItem, Long>, JpaSpecificationExecutor<FeedItem> {
    boolean existsByFeedCode(String feedCode);
    boolean existsByFeedCodeAndIdNot(String feedCode, Long id);
    
    @Query("SELECT fi FROM FeedItem fi WHERE fi.currentStock < fi.minimumStock")
    List<FeedItem> findLowStockItems();

    @Query("SELECT COALESCE(SUM(fi.currentStock), 0.0) FROM FeedItem fi")
    double sumCurrentStock();

}
