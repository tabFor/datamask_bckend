package com.example.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 在线交易实体类
 * 映射online_transactions表，包含电子商务和支付信息
 */
@Entity
@Table(name = "online_transactions")
public class OnlineTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private CustomerInfo user;

    @Column(name = "order_id", length = 20)
    private String orderId;

    @Column(name = "product_name", length = 100)
    private String productName;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @Column(name = "card_last_four", length = 4)
    private String cardLastFour;

    @Column(name = "shipping_address", length = 200)
    private String shippingAddress;

    @Column(name = "ip_address", length = 15)
    private String ipAddress;

    @Column(name = "transaction_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date transactionDate;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CustomerInfo getUser() {
        return user;
    }

    public void setUser(CustomerInfo user) {
        this.user = user;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getCardLastFour() {
        return cardLastFour;
    }

    public void setCardLastFour(String cardLastFour) {
        this.cardLastFour = cardLastFour;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Date getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate;
    }

    @Override
    public String toString() {
        return "OnlineTransaction{" +
                "id=" + id +
                ", user=" + user.getId() +
                ", orderId='" + orderId + '\'' +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", cardLastFour='" + cardLastFour + '\'' +
                ", shippingAddress='" + shippingAddress + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", transactionDate=" + transactionDate +
                '}';
    }
} 