package com.vitna;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="allocations") public class Allocation {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @ManyToOne public Domain.Order order;
 @ManyToOne public Domain.Lot lot;
 public BigDecimal quantity;
}

