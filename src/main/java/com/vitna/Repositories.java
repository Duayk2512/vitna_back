package com.vitna;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
import com.vitna.Domain.*;
interface Accounts extends JpaRepository<Account,Long> { Optional<Account> findByEmail(String email); Optional<Account> findByEmailIgnoreCase(String email); }
interface Products extends JpaRepository<Product,Long> {
 List<Product> findByPublishedTrue(); Optional<Product> findBySku(String sku); Optional<Product> findByBarcode(String barcode);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select p from #{#entityName} p where p.id = :id")
 Optional<Product> lockProduct(@org.springframework.data.repository.query.Param("id") Long id);
}
interface Lots extends JpaRepository<Lot,Long> { List<Lot> findByProductId(Long id); Optional<Lot> findByProductionId(Long id); }
interface Orders extends JpaRepository<Order,Long> { List<Order> findByCustomerId(Long id); }
interface Lines extends JpaRepository<OrderLine,Long> {List<OrderLine> findByOrderId(Long id);}
interface Productions extends JpaRepository<Production,Long> {List<Production> findByAssignedToId(Long id);}
interface ProductionSteps extends JpaRepository<ProductionStep,Long> {List<ProductionStep> findByProductionIdOrderByStepPosition(Long id);}
interface Allocations extends JpaRepository<Allocation,Long> {List<Allocation> findByOrderId(Long id);}
