package org.example.boilerserver.controller;

import org.example.boilercommon.Result;
import org.example.boilerpojo.CreateReviewDTO;
import org.example.boilerpojo.ReviewVO;
import org.example.boilerserver.service.ReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/review")
public class ReviewController {
    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/create")
    public Result<ReviewVO> createReview(@RequestBody CreateReviewDTO dto) {
        log.info("创建评价: reviewerId={}, reviewerType={}, postId={}, orderId={}, rating={}",
                dto.getReviewerId(), dto.getReviewerType(), dto.getPostId(), dto.getOrderId(), dto.getRating());
        return Result.success(reviewService.createReview(dto));
    }

    @GetMapping("/{reviewId}")
    public Result<ReviewVO> getReview(@PathVariable String reviewId) {
        log.info("查询评价详情: reviewId={}", reviewId);
        return Result.success(reviewService.getReview(reviewId));
    }

    @GetMapping("/post/{postId}")
    public Result<List<ReviewVO>> getPostReviews(@PathVariable String postId) {
        log.info("查询帖子评价列表: postId={}", postId);
        return Result.success(reviewService.getPostReviews(postId));
    }

    @GetMapping("/reviewer/{reviewerId}")
    public Result<List<ReviewVO>> listByReviewer(@PathVariable String reviewerId) {
        log.info("查询评价人发表的评价列表: reviewerId={}", reviewerId);
        return Result.success(reviewService.listByReviewer(reviewerId));
    }

    @GetMapping("/reviewee/{revieweeId}")
    public Result<List<ReviewVO>> listByReviewee(@PathVariable String revieweeId) {
        log.info("查询被评价人收到的评价列表: revieweeId={}", revieweeId);
        return Result.success(reviewService.listByReviewee(revieweeId));
    }
}
