package org.example.boilerpojo;

import lombok.Data;

@Data
public class UpdateLogisticsDTO {
    private String transactionId;
    private String sellerId;
    private String logisticsInfo;
}
