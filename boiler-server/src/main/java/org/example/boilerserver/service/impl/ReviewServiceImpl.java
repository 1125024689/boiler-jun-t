package org.example.boilerserver.service.impl;

import org.example.boilerpojo.CreateReviewDTO;
import org.example.boilerpojo.OrderEntity;
import org.example.boilerpojo.PostEntity;
import org.example.boilerpojo.ReviewEntity;
import org.example.boilerpojo.ReviewVO;
import org.example.boilerpojo.SellerEntity;
import org.example.boilerpojo.TransactionEntity;
import org.example.boilerpojo.UserEntity;
import org.example.boilerserver.mapper.OrderMapper;
import org.example.boilerserver.mapper.PostMapper;
import org.example.boilerserver.mapper.ReviewMapper;
import org.example.boilerserver.mapper.SellerMapper;
import org.example.boilerserver.mapper.TransactionMapper;
import org.example.boilerserver.mapper.UserMapper;
import org.example.boilerserver.service.ReviewService;
import org.example.boilerserver.util.CreditScoreUtils;
import org.example.constant.ReviewConstant;
import org.example.constant.TransactionConstant;
import org.example.constant.UserConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ReviewServiceImpl implements ReviewService {
    private static final Logger log = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;
    private final TransactionMapper transactionMapper;
    private final PostMapper postMapper;
    private final SellerMapper sellerMapper;
    private final UserMapper userMapper;

    public ReviewServiceImpl(ReviewMapper reviewMapper,
                             OrderMapper orderMapper,
                             TransactionMapper transactionMapper,
                             PostMapper postMapper,
                             SellerMapper sellerMapper,
                             UserMapper userMapper) {
        this.reviewMapper = reviewMapper;
        this.orderMapper = orderMapper;
        this.transactionMapper = transactionMapper;
        this.postMapper = postMapper;
        this.sellerMapper = sellerMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public ReviewVO createReview(CreateReviewDTO dto) {
        if (dto == null
                || !StringUtils.hasText(dto.getReviewerId())
                || !StringUtils.hasText(dto.getReviewerType())
                || !StringUtils.hasText(dto.getPostId())
                || !StringUtils.hasText(dto.getOrderId())
                || dto.getRating() == null) {
            throw new IllegalArgumentException("评价人ID、评价人类型、帖子ID、订单ID和评分不能为空");
        }

        // 校验评分范围
        int rating = dto.getRating();
        if (rating < ReviewConstant.MIN_RATING || rating > ReviewConstant.MAX_RATING) {
            throw new IllegalArgumentException("评分必须在" + ReviewConstant.MIN_RATING + "-" + ReviewConstant.MAX_RATING + "之间");
        }

        // 校验评价人存在且类型匹配
        UserEntity reviewerUser = userMapper.getByUserId(dto.getReviewerId());
        if (reviewerUser == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (!dto.getReviewerType().equalsIgnoreCase(reviewerUser.getUserType())) {
            throw new IllegalArgumentException("评价人类型与用户类型不匹配");
        }

        // 校验帖子存在
        PostEntity postEntity = postMapper.getByPostId(dto.getPostId());
        if (postEntity == null) {
            throw new IllegalArgumentException("帖子不存在");
        }

        // 校验订单存在且为已完成状态
        OrderEntity orderEntity = orderMapper.getByOrderId(dto.getOrderId());
        if (orderEntity == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!TransactionConstant.ORDER_STATUS_COMPLETED.equals(orderEntity.getOrderStatus())) {
            throw new IllegalArgumentException("订单未完成，不能评价");
        }

        // 校验该订单对应的交易存在
        TransactionEntity transactionEntity = transactionMapper.getByTransactionId(orderEntity.getTransactionId());
        if (transactionEntity == null) {
            throw new IllegalArgumentException("交易不存在");
        }

        // 根据评价人类型确定被评价人
        String revieweeId;
        if (UserConstant.USER_TYPE_BUYER.equalsIgnoreCase(dto.getReviewerType())) {
            // 买家评价卖家
            if (!transactionEntity.getBuyerId().equals(dto.getReviewerId())) {
                throw new IllegalArgumentException("只有该订单的买家可以评价卖家");
            }
            revieweeId = transactionEntity.getSellerId();
        } else if (UserConstant.USER_TYPE_SELLER.equalsIgnoreCase(dto.getReviewerType())) {
            // 卖家评价买家
            if (!transactionEntity.getSellerId().equals(dto.getReviewerId())) {
                throw new IllegalArgumentException("只有该订单的卖家可以评价买家");
            }
            revieweeId = transactionEntity.getBuyerId();
        } else {
            throw new IllegalArgumentException("不支持的评价人类型: " + dto.getReviewerType());
        }

        // 校验该评价人未对此订单重复评价
        ReviewEntity existingReview = reviewMapper.getByReviewerIdAndOrderId(dto.getReviewerId(), dto.getOrderId());
        if (existingReview != null) {
            throw new IllegalArgumentException("您已对此订单进行过评价");
        }

        // 创建评价记录
        String reviewId = UUID.randomUUID().toString().replace("-", "");
        ReviewEntity reviewEntity = new ReviewEntity();
        reviewEntity.setReviewId(reviewId);
        reviewEntity.setReviewerId(dto.getReviewerId());
        reviewEntity.setRevieweeId(revieweeId);
        reviewEntity.setReviewerType(dto.getReviewerType().toUpperCase());
        reviewEntity.setPostId(dto.getPostId());
        reviewEntity.setOrderId(dto.getOrderId());
        reviewEntity.setRating(rating);
        reviewEntity.setContent(dto.getContent());
        reviewEntity.setReviewTime(LocalDate.now());
        reviewMapper.insert(reviewEntity);

        // 更新被评价人的互评信用分：好评 +3（上限30），差评 -5（下限0）
        UserEntity revieweeUser = userMapper.getByUserId(revieweeId);
        if (revieweeUser != null) {
            int currentMutualScore = revieweeUser.getMutualRatingScore() == null
                    ? UserConstant.INITIAL_MUTUAL_RATING_SCORE : revieweeUser.getMutualRatingScore();
            int creditChange = rating >= ReviewConstant.POSITIVE_RATING_THRESHOLD
                    ? ReviewConstant.CREDIT_SCORE_POSITIVE
                    : ReviewConstant.CREDIT_SCORE_NEGATIVE;
            int newMutualScore = Math.max(UserConstant.MIN_MUTUAL_RATING_SCORE,
                    Math.min(currentMutualScore + creditChange, UserConstant.MAX_MUTUAL_RATING_SCORE));
            revieweeUser.setMutualRatingScore(newMutualScore);

            // 重新计算总信用分
            int oldTotalCredit = revieweeUser.getCreditScore() == null ? 0 : revieweeUser.getCreditScore();
            int totalCredit = CreditScoreUtils.calculateTotalCreditScore(revieweeUser);
            revieweeUser.setCreditScore(totalCredit);
            userMapper.update(revieweeUser);
            log.info("【信用分更新】评价: revieweeId={}, reviewerId={}, reviewerType={}, rating={}, 互评分 {}→{}, 总信用分 {}→{}, reviewId={}",
                    revieweeId, dto.getReviewerId(), dto.getReviewerType(), rating,
                    currentMutualScore, newMutualScore, oldTotalCredit, totalCredit, reviewId);
        }

        // 若被评价方是卖家（即买家评价卖家），更新卖家好评率
        if (UserConstant.USER_TYPE_BUYER.equalsIgnoreCase(dto.getReviewerType())) {
            updateSellerPositiveRatingRate(revieweeId);
        }

        return buildReviewVO(reviewEntity, reviewerUser, revieweeUser, postEntity);
    }

    @Override
    public ReviewVO getReview(String reviewId) {
        if (!StringUtils.hasText(reviewId)) {
            throw new IllegalArgumentException("评价ID不能为空");
        }
        ReviewEntity reviewEntity = reviewMapper.getByReviewId(reviewId);
        if (reviewEntity == null) {
            throw new IllegalArgumentException("评价不存在");
        }
        UserEntity reviewerUser = userMapper.getByUserId(reviewEntity.getReviewerId());
        UserEntity revieweeUser = userMapper.getByUserId(reviewEntity.getRevieweeId());
        PostEntity postEntity = postMapper.getByPostId(reviewEntity.getPostId());
        return buildReviewVO(reviewEntity, reviewerUser, revieweeUser, postEntity);
    }

    @Override
    public List<ReviewVO> getPostReviews(String postId) {
        if (!StringUtils.hasText(postId)) {
            throw new IllegalArgumentException("帖子ID不能为空");
        }
        return reviewMapper.listByPostId(postId).stream()
                .map(r -> {
                    UserEntity reviewerUser = userMapper.getByUserId(r.getReviewerId());
                    UserEntity revieweeUser = userMapper.getByUserId(r.getRevieweeId());
                    PostEntity postEntity = postMapper.getByPostId(r.getPostId());
                    return buildReviewVO(r, reviewerUser, revieweeUser, postEntity);
                })
                .toList();
    }

    @Override
    public List<ReviewVO> listByReviewer(String reviewerId) {
        if (!StringUtils.hasText(reviewerId)) {
            throw new IllegalArgumentException("评价人ID不能为空");
        }
        return reviewMapper.listByReviewerId(reviewerId).stream()
                .map(r -> {
                    UserEntity reviewerUser = userMapper.getByUserId(r.getReviewerId());
                    UserEntity revieweeUser = userMapper.getByUserId(r.getRevieweeId());
                    PostEntity postEntity = postMapper.getByPostId(r.getPostId());
                    return buildReviewVO(r, reviewerUser, revieweeUser, postEntity);
                })
                .toList();
    }

    @Override
    public List<ReviewVO> listByReviewee(String revieweeId) {
        if (!StringUtils.hasText(revieweeId)) {
            throw new IllegalArgumentException("被评价人ID不能为空");
        }
        return reviewMapper.listByRevieweeId(revieweeId).stream()
                .map(r -> {
                    UserEntity reviewerUser = userMapper.getByUserId(r.getReviewerId());
                    UserEntity revieweeUser = userMapper.getByUserId(r.getRevieweeId());
                    PostEntity postEntity = postMapper.getByPostId(r.getPostId());
                    return buildReviewVO(r, reviewerUser, revieweeUser, postEntity);
                })
                .toList();
    }

    private void updateSellerPositiveRatingRate(String sellerId) {
        SellerEntity sellerEntity = sellerMapper.getBySellerId(sellerId);
        if (sellerEntity == null) {
            return;
        }
        int totalCount = reviewMapper.countByRevieweeId(sellerId);
        int positiveCount = reviewMapper.countPositiveByRevieweeId(sellerId);
        BigDecimal positiveRate = totalCount == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(positiveCount)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalCount), 2, BigDecimal.ROUND_HALF_UP);
        sellerEntity.setPositiveRatingRate(positiveRate);
        sellerMapper.update(sellerEntity);
        log.info("【好评率更新】sellerId={}, 总评价数={}, 好评数={}, 好评率={}%",
                sellerId, totalCount, positiveCount, positiveRate);
    }

    private ReviewVO buildReviewVO(ReviewEntity reviewEntity, UserEntity reviewerUser, UserEntity revieweeUser, PostEntity postEntity) {
        ReviewVO vo = new ReviewVO();
        vo.setReviewId(reviewEntity.getReviewId());
        vo.setReviewerId(reviewEntity.getReviewerId());
        vo.setReviewerType(reviewEntity.getReviewerType());
        vo.setRevieweeId(reviewEntity.getRevieweeId());
        vo.setPostId(reviewEntity.getPostId());
        vo.setOrderId(reviewEntity.getOrderId());
        vo.setRating(reviewEntity.getRating());
        vo.setContent(reviewEntity.getContent());
        vo.setReviewTime(reviewEntity.getReviewTime());

        if (reviewerUser != null) {
            vo.setReviewerName(reviewerUser.getUsername());
        }

        if (revieweeUser != null) {
            vo.setRevieweeName(revieweeUser.getUsername());
        }

        if (postEntity != null) {
            vo.setPostTitle(postEntity.getTitle());
        }

        return vo;
    }
}
