package org.example.boilerpojo;

import lombok.Data;

@Data
public class CreateReviewDTO {
    private String reviewerId;
    private String reviewerType;
    private String postId;
    private String orderId;
    private Integer rating;
    private String content;
}
