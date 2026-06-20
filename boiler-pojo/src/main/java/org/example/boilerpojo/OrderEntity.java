package org.example.boilerpojo;

import lombok.Data;
import java.time.LocalDate;

@Data
public class OrderEntity {
    private String orderId;
    private String transactionId;
    private String orderStatus;
    private LocalDate createTime;
    private LocalDate updateTime;
}
