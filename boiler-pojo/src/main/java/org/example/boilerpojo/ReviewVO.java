package org.example.boilerpojo;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ReviewVO {
    private String reviewId;
    private String reviewerId;
    private String reviewerName;
    private String reviewerType;
    private String revieweeId;
    private String revieweeName;
    private String postId;
    private String postTitle;
    private String orderId;
    private Integer rating;
    private String content;
    private LocalDate reviewTime;
}
