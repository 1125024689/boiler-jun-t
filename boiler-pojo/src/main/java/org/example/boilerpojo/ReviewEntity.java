package org.example.boilerpojo;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ReviewEntity {
    private String reviewId;
    private String reviewerId;
    private String revieweeId;
    private String reviewerType;
    private String postId;
    private String orderId;
    private Integer rating;
    private String content;
    private LocalDate reviewTime;
}
