package com.example.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 金融记录实体类
 * 映射financial_records表，包含银行和财务信息
 */
@Entity
@Table(name = "financial_records")
public class FinancialRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private CustomerInfo customer;

    @Column(name = "account_number", length = 20)
    private String accountNumber;

    @Column(name = "card_number", length = 19)
    private String cardNumber;

    @Column(name = "card_cvv", length = 4)
    private String cardCvv;

    @Column(name = "card_expiry", length = 7)
    private String cardExpiry;

    @Column(name = "balance", precision = 12, scale = 2)
    private BigDecimal balance;

    @Column(name = "income", precision = 12, scale = 2)
    private BigDecimal income;

    @Column(name = "transaction_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date transactionDate;

    @Column(name = "transaction_type", length = 20)
    private String transactionType;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CustomerInfo getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerInfo customer) {
        this.customer = customer;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getCardCvv() {
        return cardCvv;
    }

    public void setCardCvv(String cardCvv) {
        this.cardCvv = cardCvv;
    }

    public String getCardExpiry() {
        return cardExpiry;
    }

    public void setCardExpiry(String cardExpiry) {
        this.cardExpiry = cardExpiry;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getIncome() {
        return income;
    }

    public void setIncome(BigDecimal income) {
        this.income = income;
    }

    public Date getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    @Override
    public String toString() {
        return "FinancialRecord{" +
                "id=" + id +
                ", customer=" + customer.getId() +
                ", accountNumber='" + accountNumber + '\'' +
                ", cardNumber='" + cardNumber + '\'' +
                ", cardCvv='" + cardCvv + '\'' +
                ", cardExpiry='" + cardExpiry + '\'' +
                ", balance=" + balance +
                ", income=" + income +
                ", transactionDate=" + transactionDate +
                ", transactionType='" + transactionType + '\'' +
                '}';
    }
} 