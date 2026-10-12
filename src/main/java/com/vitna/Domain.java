package com.vitna;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
public class Domain {
 public enum Role { ADMIN, CLIENTE, ALMACENERO, JEFE_PRODUCCION, TRABAJADOR, CALIDAD }
 @Entity @Table(name="accounts") public static class Account {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  public String name;
  @Column(unique=true,nullable=false) public String email;
  public String passwordHash, phone, document, address;
  @Enumerated(EnumType.STRING) public Role role;
  public boolean active=true;
 }
 @Entity @Table(name="products") public static class Product {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @Column(unique=true) public String sku;
  @Column(unique=true,nullable=false,length=48) public String barcode;
  @PrePersist void ensureBarcode(){if(barcode==null||barcode.isBlank())barcode=newBarcode();}
  static String newBarcode(){return "VT"+java.util.UUID.randomUUID().toString().replace("-", "").toUpperCase(java.util.Locale.ROOT);}
  public String name, category, presentation;
  @Column(length=2000) public String description;
  public BigDecimal price=BigDecimal.ZERO;
  public boolean published;
  @Transient public BigDecimal availableStock;
 }
 @Entity @Table(name="lots") public static class Lot {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @ManyToOne(optional=false) public Product product;
  public String code, warehouse;
  @Column(unique=true) public Long productionId;
  public LocalDate expiry;
  public BigDecimal quantity=BigDecimal.ZERO, reserved=BigDecimal.ZERO;
  public String status="CUARENTENA";
  @Column(length=1000) public String qualityNote;
  public String reviewedBy;
  public java.time.Instant reviewedAt;
  @Version public long version;
 }
 @Entity(name="CustomerOrder") @Table(name="customer_orders") public static class Order {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @ManyToOne public Account customer;
  public String status="PENDIENTE_REVISION", address;
  @Version public long version;
  public BigDecimal total=BigDecimal.ZERO;
 }
 @Entity @Table(name="order_lines") public static class OrderLine {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @ManyToOne public Order order;
  @ManyToOne public Product product;
  public BigDecimal quantity, unitPrice;
 }
 @Entity @Table(name="production_orders") public static class Production {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @ManyToOne public Product product;
  public BigDecimal planned;
  public String route, status="PLANIFICADA";
  @ManyToOne public Account assignedTo;
  public BigDecimal produced=BigDecimal.ZERO;
  @Version public long version;
 }
 @Entity @Table(name="production_steps") public static class ProductionStep {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @ManyToOne(optional=false) public Production production;
  public String name, status="PENDIENTE";
  public int stepPosition;
  public java.time.Instant startedAt, completedAt;
 }
}


