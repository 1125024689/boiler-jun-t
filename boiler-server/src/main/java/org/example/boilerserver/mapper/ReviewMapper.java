package org.example.boilerserver.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.boilerpojo.ReviewEntity;

import java.util.List;

@Mapper
public interface ReviewMapper {
    int insert(ReviewEntity entity);

    ReviewEntity getByReviewId(String reviewId);

    List<ReviewEntity> listByPostId(String postId);

    List<ReviewEntity> listByReviewerId(String reviewerId);

    List<ReviewEntity> listByRevieweeId(String revieweeId);

    ReviewEntity getByReviewerIdAndOrderId(String reviewerId, String orderId);

    int countByPostId(String postId);

    int countPositiveByPostId(String postId);

    int countByRevieweeId(String revieweeId);

    int countPositiveByRevieweeId(String revieweeId);
}
