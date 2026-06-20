package org.example.boilerserver.service.impl;

import org.example.boilerpojo.BookPostDTO;
import org.example.boilerpojo.BuyerEntity;
import org.example.boilerpojo.CancelBookingDTO;
import org.example.boilerpojo.CompleteTransactionDTO;
import org.example.boilerpojo.OrderEntity;
import org.example.boilerpojo.PostEntity;
import org.example.boilerpojo.SellerEntity;
import org.example.boilerpojo.TransactionEntity;
import org.example.boilerpojo.TransactionVO;
import org.example.boilerpojo.UpdateLogisticsDTO;
import org.example.boilerpojo.UserEntity;
import org.example.boilerserver.mapper.BuyerMapper;
import org.example.boilerserver.mapper.OrderMapper;
import org.example.boilerserver.mapper.PostMapper;
import org.example.boilerserver.mapper.SellerMapper;
import org.example.boilerserver.mapper.TransactionMapper;
import org.example.boilerserver.mapper.UserMapper;
import org.example.boilerserver.service.TransactionService;
import org.example.constant.TransactionConstant;
import org.example.constant.UserConstant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionServiceImpl implements TransactionService {
    private static final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);

    private final TransactionMapper transactionMapper;
    private final OrderMapper orderMapper;
    private final PostMapper postMapper;
    private final BuyerMapper buyerMapper;
    private final SellerMapper sellerMapper;
    private final UserMapper userMapper;

    public TransactionServiceImpl(TransactionMapper transactionMapper,
                                  OrderMapper orderMapper,
                                  PostMapper postMapper,
                                  BuyerMapper buyerMapper,
                                  SellerMapper sellerMapper,
                                  UserMapper userMapper) {
        this.transactionMapper = transactionMapper;
        this.orderMapper = orderMapper;
        this.postMapper = postMapper;
        this.buyerMapper = buyerMapper;
        this.sellerMapper = sellerMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public TransactionVO bookPost(BookPostDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getBuyerId()) || !StringUtils.hasText(dto.getPostId())) {
            throw new IllegalArgumentException("买家ID和帖子ID不能为空");
        }

        // 校验买家存在且为买家类型
        UserEntity buyerUser = userMapper.getByUserId(dto.getBuyerId());
        if (buyerUser == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (!UserConstant.USER_TYPE_BUYER.equalsIgnoreCase(buyerUser.getUserType())) {
            throw new IllegalArgumentException("当前用户不是买家");
        }
        BuyerEntity buyerEntity = buyerMapper.getByBuyerId(dto.getBuyerId());
        if (buyerEntity == null) {
            throw new IllegalArgumentException("买家信息不存在");
        }

        // 校验帖子存在且为上架状态
        PostEntity postEntity = postMapper.getByPostId(dto.getPostId());
        if (postEntity == null) {
            throw new IllegalArgumentException("帖子不存在");
        }
        if (!TransactionConstant.POST_STATUS_AVAILABLE.equals(postEntity.getStatus())) {
            throw new IllegalArgumentException("该帖子当前不可预约");
        }

        // 校验该帖子未被当前买家预约（避免重复预约）
        TransactionEntity existingTransaction = transactionMapper.getByBuyerIdAndPostId(dto.getBuyerId(), dto.getPostId());
        if (existingTransaction != null
                && TransactionConstant.BOOKING_STATUS_BOOKED.equals(existingTransaction.getBookingStatus())) {
            throw new IllegalArgumentException("您已预约该帖子，请勿重复预约");
        }

        // 创建交易记录
        String transactionId = UUID.randomUUID().toString().replace("-", "");
        TransactionEntity transactionEntity = new TransactionEntity();
        transactionEntity.setTransactionId(transactionId);
        transactionEntity.setBuyerId(dto.getBuyerId());
        transactionEntity.setSellerId(postEntity.getSellerId());
        transactionEntity.setTransactionAmount(postEntity.getPrice());
        transactionEntity.setTransactionStatus(TransactionConstant.TRANSACTION_STATUS_PENDING);
        transactionEntity.setBookingStatus(TransactionConstant.BOOKING_STATUS_BOOKED);
        transactionEntity.setPostId(dto.getPostId());
        transactionMapper.insert(transactionEntity);

        // 创建关联订单
        String orderId = UUID.randomUUID().toString().replace("-", "");
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderId(orderId);
        orderEntity.setTransactionId(transactionId);
        orderEntity.setOrderStatus(TransactionConstant.ORDER_STATUS_CREATED);
        orderEntity.setCreateTime(LocalDate.now());
        orderEntity.setUpdateTime(LocalDate.now());
        orderMapper.insert(orderEntity);

        // 预约成功后将帖子标记为已预约，防止其他买家重复预约
        log.info("【帖子状态流转】预约成功，帖子状态变更: postId={}, 原状态={}, 新状态={}, buyerId={}, transactionId={}",
                dto.getPostId(), TransactionConstant.POST_STATUS_AVAILABLE, TransactionConstant.POST_STATUS_BOOKED,
                dto.getBuyerId(), transactionId);
        postMapper.updateStatus(dto.getPostId(), TransactionConstant.POST_STATUS_BOOKED);

        return buildTransactionVO(transactionEntity, orderEntity, postEntity);
    }

    @Override
    @Transactional
    public TransactionVO cancelBooking(CancelBookingDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getTransactionId()) || !StringUtils.hasText(dto.getBuyerId())) {
            throw new IllegalArgumentException("交易ID和买家ID不能为空");
        }

        TransactionEntity transactionEntity = getExistingTransaction(dto.getTransactionId());

        // 只有买家本人可以取消预约
        if (!transactionEntity.getBuyerId().equals(dto.getBuyerId())) {
            throw new IllegalArgumentException("只有买家本人可以取消预约");
        }

        // 校验当前状态为已预约
        if (!TransactionConstant.BOOKING_STATUS_BOOKED.equals(transactionEntity.getBookingStatus())) {
            throw new IllegalArgumentException("当前交易状态不允许取消预约");
        }

        // 更新交易状态
        transactionEntity.setBookingStatus(TransactionConstant.BOOKING_STATUS_CANCELLED);
        transactionEntity.setTransactionStatus(TransactionConstant.TRANSACTION_STATUS_CANCELLED);
        transactionMapper.update(transactionEntity);

        // 更新订单状态
        OrderEntity orderEntity = orderMapper.getByTransactionId(dto.getTransactionId());
        if (orderEntity != null) {
            orderEntity.setOrderStatus(TransactionConstant.ORDER_STATUS_CANCELLED);
            orderEntity.setUpdateTime(LocalDate.now());
            orderMapper.update(orderEntity);
        }

        // 取消预约后恢复帖子为可预约状态
        if (StringUtils.hasText(transactionEntity.getPostId())) {
            log.info("【帖子状态流转】取消预约，帖子状态变更: postId={}, 原状态={}, 新状态={}, buyerId={}, transactionId={}",
                    transactionEntity.getPostId(), TransactionConstant.POST_STATUS_BOOKED, TransactionConstant.POST_STATUS_AVAILABLE,
                    dto.getBuyerId(), dto.getTransactionId());
            postMapper.updateStatus(transactionEntity.getPostId(), TransactionConstant.POST_STATUS_AVAILABLE);
        }

        return buildTransactionVO(transactionEntity, orderEntity, null);
    }

    @Override
    @Transactional
    public TransactionVO completeTransaction(CompleteTransactionDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getTransactionId()) || !StringUtils.hasText(dto.getSellerId())) {
            throw new IllegalArgumentException("交易ID和卖家ID不能为空");
        }

        TransactionEntity transactionEntity = getExistingTransaction(dto.getTransactionId());

        // 校验操作者是该交易的卖家
        if (!transactionEntity.getSellerId().equals(dto.getSellerId())) {
            throw new IllegalArgumentException("只有该交易的卖家可以完成交易");
        }

        // 校验交易状态为待交易
        if (!TransactionConstant.TRANSACTION_STATUS_PENDING.equals(transactionEntity.getTransactionStatus())) {
            throw new IllegalArgumentException("当前交易状态不允许完成交易");
        }

        // 更新交易状态为已完成
        transactionEntity.setTransactionStatus(TransactionConstant.TRANSACTION_STATUS_COMPLETED);
        transactionEntity.setTransactionTime(LocalDate.now());
        transactionMapper.update(transactionEntity);

        // 更新订单状态为已完成
        OrderEntity orderEntity = orderMapper.getByTransactionId(dto.getTransactionId());
        if (orderEntity != null) {
            orderEntity.setOrderStatus(TransactionConstant.ORDER_STATUS_COMPLETED);
            orderEntity.setUpdateTime(LocalDate.now());
            orderMapper.update(orderEntity);
        }

        // 将关联帖子标记为已售出
        if (StringUtils.hasText(transactionEntity.getPostId())) {
            log.info("【帖子状态流转】交易完成，帖子状态变更: postId={}, 原状态={}, 新状态={}, sellerId={}, buyerId={}, transactionId={}",
                    transactionEntity.getPostId(), TransactionConstant.POST_STATUS_BOOKED, TransactionConstant.POST_STATUS_SOLD,
                    dto.getSellerId(), transactionEntity.getBuyerId(), dto.getTransactionId());
            postMapper.updateStatus(transactionEntity.getPostId(), TransactionConstant.POST_STATUS_SOLD);
        }

        // 更新卖家完成交易数和信用分
        SellerEntity sellerEntity = sellerMapper.getBySellerId(dto.getSellerId());
        if (sellerEntity != null) {
            int completedCount = sellerEntity.getCompletedTransactionCount() == null
                    ? 0 : sellerEntity.getCompletedTransactionCount();
            sellerEntity.setCompletedTransactionCount(completedCount + 1);
            sellerMapper.update(sellerEntity);

            // 更新卖家信用分 +2
            UserEntity sellerUser = userMapper.getByUserId(dto.getSellerId());
            if (sellerUser != null) {
                int currentCredit = sellerUser.getCreditScore() == null
                        ? UserConstant.DEFAULT_CREDIT_SCORE : sellerUser.getCreditScore();
                int newCredit = Math.min(currentCredit + TransactionConstant.CREDIT_SCORE_PER_TRANSACTION,
                        UserConstant.MAX_CREDIT_SCORE);
                sellerUser.setCreditScore(newCredit);
                userMapper.update(sellerUser);
                log.info("【信用分更新】卖家: sellerId={}, 原信用分={}, 新信用分={}, 交易ID={}",
                        dto.getSellerId(), currentCredit, newCredit, dto.getTransactionId());
            }
        }

        // 更新买家信用分 +2（需求文档：交易行为 - 每笔成功交易 +2 分，买卖双方均适用）
        UserEntity buyerUser = userMapper.getByUserId(transactionEntity.getBuyerId());
        if (buyerUser != null) {
            int currentCredit = buyerUser.getCreditScore() == null
                    ? UserConstant.DEFAULT_CREDIT_SCORE : buyerUser.getCreditScore();
            int newCredit = Math.min(currentCredit + TransactionConstant.CREDIT_SCORE_PER_TRANSACTION,
                    UserConstant.MAX_CREDIT_SCORE);
            buyerUser.setCreditScore(newCredit);
            userMapper.update(buyerUser);
            log.info("【信用分更新】买家: buyerId={}, 原信用分={}, 新信用分={}, 交易ID={}",
                    transactionEntity.getBuyerId(), currentCredit, newCredit, dto.getTransactionId());
        }

        PostEntity postEntity = StringUtils.hasText(transactionEntity.getPostId())
                ? postMapper.getByPostId(transactionEntity.getPostId()) : null;
        return buildTransactionVO(transactionEntity, orderEntity, postEntity);
    }

    @Override
    @Transactional
    public TransactionVO updateLogistics(UpdateLogisticsDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getTransactionId()) || !StringUtils.hasText(dto.getSellerId())) {
            throw new IllegalArgumentException("交易ID和卖家ID不能为空");
        }
        if (!StringUtils.hasText(dto.getLogisticsInfo())) {
            throw new IllegalArgumentException("物流信息不能为空");
        }

        TransactionEntity transactionEntity = getExistingTransaction(dto.getTransactionId());

        // 校验操作者是该交易的卖家
        if (!transactionEntity.getSellerId().equals(dto.getSellerId())) {
            throw new IllegalArgumentException("只有该交易的卖家可以更新物流信息");
        }

        // 校验交易未被取消
        if (TransactionConstant.TRANSACTION_STATUS_CANCELLED.equals(transactionEntity.getTransactionStatus())) {
            throw new IllegalArgumentException("已取消的交易不能更新物流信息");
        }

        transactionEntity.setLogisticsInfo(dto.getLogisticsInfo().trim());
        transactionMapper.update(transactionEntity);

        OrderEntity orderEntity = orderMapper.getByTransactionId(dto.getTransactionId());
        return buildTransactionVO(transactionEntity, orderEntity, null);
    }

    @Override
    public TransactionVO getTransaction(String transactionId) {
        TransactionEntity transactionEntity = getExistingTransaction(transactionId);
        OrderEntity orderEntity = orderMapper.getByTransactionId(transactionId);
        PostEntity postEntity = StringUtils.hasText(transactionEntity.getPostId())
                ? postMapper.getByPostId(transactionEntity.getPostId()) : null;
        return buildTransactionVO(transactionEntity, orderEntity, postEntity);
    }

    @Override
    public List<TransactionVO> listByBuyer(String buyerId) {
        if (!StringUtils.hasText(buyerId)) {
            throw new IllegalArgumentException("买家ID不能为空");
        }
        return transactionMapper.listByBuyerId(buyerId).stream()
                .map(t -> {
                    OrderEntity orderEntity = orderMapper.getByTransactionId(t.getTransactionId());
                    PostEntity postEntity = StringUtils.hasText(t.getPostId())
                            ? postMapper.getByPostId(t.getPostId()) : null;
                    return buildTransactionVO(t, orderEntity, postEntity);
                })
                .toList();
    }

    @Override
    public List<TransactionVO> listBySeller(String sellerId) {
        if (!StringUtils.hasText(sellerId)) {
            throw new IllegalArgumentException("卖家ID不能为空");
        }
        return transactionMapper.listBySellerId(sellerId).stream()
                .map(t -> {
                    OrderEntity orderEntity = orderMapper.getByTransactionId(t.getTransactionId());
                    PostEntity postEntity = StringUtils.hasText(t.getPostId())
                            ? postMapper.getByPostId(t.getPostId()) : null;
                    return buildTransactionVO(t, orderEntity, postEntity);
                })
                .toList();
    }

    private TransactionEntity getExistingTransaction(String transactionId) {
        if (!StringUtils.hasText(transactionId)) {
            throw new IllegalArgumentException("交易ID不能为空");
        }
        TransactionEntity transactionEntity = transactionMapper.getByTransactionId(transactionId);
        if (transactionEntity == null) {
            throw new IllegalArgumentException("交易不存在");
        }
        return transactionEntity;
    }

    private TransactionVO buildTransactionVO(TransactionEntity transactionEntity, OrderEntity orderEntity, PostEntity postEntity) {
        TransactionVO vo = new TransactionVO();
        vo.setTransactionId(transactionEntity.getTransactionId());
        vo.setBuyerId(transactionEntity.getBuyerId());
        vo.setSellerId(transactionEntity.getSellerId());
        vo.setPostId(transactionEntity.getPostId());
        vo.setTransactionAmount(transactionEntity.getTransactionAmount());
        vo.setTransactionTime(transactionEntity.getTransactionTime());
        vo.setTransactionStatus(transactionEntity.getTransactionStatus());
        vo.setBookingStatus(transactionEntity.getBookingStatus());
        vo.setLogisticsInfo(transactionEntity.getLogisticsInfo());

        if (postEntity != null) {
            vo.setPostTitle(postEntity.getTitle());
        }

        if (orderEntity != null) {
            vo.setOrderId(orderEntity.getOrderId());
            vo.setOrderStatus(orderEntity.getOrderStatus());
            vo.setOrderCreateTime(orderEntity.getCreateTime());
            vo.setOrderUpdateTime(orderEntity.getUpdateTime());
        }

        return vo;
    }
}
