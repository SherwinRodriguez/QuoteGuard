package com.quoteguard.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table (name = "invoice_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItems {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String product;
    private int quantity;
    private double unitPrice;

    public Double getLineTotal() {
        return unitPrice * quantity;
    }

    @ManyToOne
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

}
