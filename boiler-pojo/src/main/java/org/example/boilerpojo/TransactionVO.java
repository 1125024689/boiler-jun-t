package org.example.boilerpojo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionVO {
    private String transactionId;
    private String buyerId;
    private String sellerId;
    private String postId;
    private String postTitle;
    private BigDecimal transactionAmount;
    private LocalDate transactionTime;
    private String transactionStatus;
    private String bookingStatus;
    private String logisticsInfo;
    private String orderId;
    private String orderStatus;
    private LocalDate orderCreateTime;
    private LocalDate orderUpdateTime;
}
