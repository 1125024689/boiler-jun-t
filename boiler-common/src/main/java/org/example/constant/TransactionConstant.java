package org.example.constant;

public final class TransactionConstant {
    // 预约状态 bookingStatus
    public static final String BOOKING_STATUS_BOOKED = "BOOKED";
    public static final String BOOKING_STATUS_CANCELLED = "CANCELLED";

    // 交易状态 transactionStatus
    public static final String TRANSACTION_STATUS_PENDING = "PENDING";
    public static final String TRANSACTION_STATUS_COMPLETED = "COMPLETED";
    public static final String TRANSACTION_STATUS_CANCELLED = "CANCELLED";

    // 订单状态 orderStatus
    public static final String ORDER_STATUS_CREATED = "CREATED";
    public static final String ORDER_STATUS_COMPLETED = "COMPLETED";
    public static final String ORDER_STATUS_CANCELLED = "CANCELLED";

    // 帖子状态 postStatus
    public static final String POST_STATUS_AVAILABLE = "AVAILABLE";
    public static final String POST_STATUS_BOOKED = "BOOKED";
    public static final String POST_STATUS_OFFLINE = "OFFLINE";
    public static final String POST_STATUS_SOLD = "SOLD";

    // 信用分规则：每笔成功交易 +2 分
    public static final int CREDIT_SCORE_PER_TRANSACTION = 2;

    private TransactionConstant() {
    }
}
