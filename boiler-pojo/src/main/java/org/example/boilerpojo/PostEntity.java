package org.example.boilerpojo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PostEntity {
    private String postId;
    private String sellerId;
    private String title;
    private BigDecimal price;
    private String description;
    private String status;
    private String city;
    private String boilerId;
}
