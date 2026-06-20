package org.example.boilerserver.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.boilerpojo.TransactionEntity;

import java.util.List;

@Mapper
public interface TransactionMapper {
    int insert(TransactionEntity entity);

    int update(TransactionEntity entity);

    TransactionEntity getByTransactionId(String transactionId);

    List<TransactionEntity> listByBuyerId(String buyerId);

    List<TransactionEntity> listBySellerId(String sellerId);

    TransactionEntity getByBuyerIdAndPostId(String buyerId, String postId);
}
