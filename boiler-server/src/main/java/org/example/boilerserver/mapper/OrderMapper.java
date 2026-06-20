package org.example.boilerserver.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.boilerpojo.OrderEntity;

@Mapper
public interface OrderMapper {
    int insert(OrderEntity entity);

    int update(OrderEntity entity);

    OrderEntity getByOrderId(String orderId);

    OrderEntity getByTransactionId(String transactionId);
}
