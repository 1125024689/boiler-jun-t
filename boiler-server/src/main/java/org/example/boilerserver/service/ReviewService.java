package org.example.boilerserver.service;

import org.example.boilerpojo.CreateReviewDTO;
import org.example.boilerpojo.ReviewVO;

import java.util.List;

public interface ReviewService {
    ReviewVO createReview(CreateReviewDTO dto);

    ReviewVO getReview(String reviewId);

    List<ReviewVO> getPostReviews(String postId);

    List<ReviewVO> listByReviewer(String reviewerId);

    List<ReviewVO> listByReviewee(String revieweeId);
}
