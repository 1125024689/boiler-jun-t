package org.example.boilerpojo;

import lombok.Data;

@Data
public class CancelBookingDTO {
    private String transactionId;
    private String buyerId;
}
