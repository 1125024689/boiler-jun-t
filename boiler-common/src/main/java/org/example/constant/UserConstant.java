package org.example.constant;

public final class UserConstant {
    public static final String USER_TYPE_BUYER = "BUYER";
    public static final String USER_TYPE_SELLER = "SELLER";
    public static final String USER_TYPE_ADMIN = "ADMIN";

    public static final String VERIFICATION_STATUS_UNVERIFIED = "UNVERIFIED";
    public static final String VERIFICATION_STATUS_VERIFIED = "VERIFIED";
    public static final String VERIFICATION_STATUS_SUSPENDED = "SUSPENDED";

    public static final String QUALIFICATION_STATUS_PENDING = "PENDING";
    public static final String QUALIFICATION_STATUS_APPROVED = "APPROVED";
    public static final String QUALIFICATION_STATUS_REJECTED = "REJECTED";

    public static final int DEFAULT_CREDIT_SCORE = 60;
    public static final int MAX_CREDIT_SCORE = 100;
    public static final int MIN_CREDIT_SCORE = 0;
    public static final int SELLER_APPROVAL_CREDIT_SCORE = 80;

    // 信用分组件初始值（需求文档：Deposit 0, Info 0, Mutual 15, Transaction 10, Community 10）
    public static final int INITIAL_DEPOSIT_PAYMENT_SCORE = 0;
    public static final int INITIAL_INFO_COMPLETENESS_SCORE = 0;
    public static final int INITIAL_MUTUAL_RATING_SCORE = 15;
    public static final int INITIAL_TRANSACTION_BEHAVIOR_SCORE = 10;
    public static final int INITIAL_COMMUNITY_CONDUCT_SCORE = 10;

    // 信用分组件上限
    public static final int MAX_DEPOSIT_PAYMENT_SCORE = 20;
    public static final int MAX_INFO_COMPLETENESS_SCORE = 20;
    public static final int MAX_MUTUAL_RATING_SCORE = 30;
    public static final int MAX_TRANSACTION_BEHAVIOR_SCORE = 20;
    public static final int MAX_COMMUNITY_CONDUCT_SCORE = 10;

    // 信用分组件下限
    public static final int MIN_MUTUAL_RATING_SCORE = 0;

    public static final String DEFAULT_VERIFICATION_STATUS = VERIFICATION_STATUS_UNVERIFIED;
    public static final String DEFAULT_QUALIFICATION_STATUS = QUALIFICATION_STATUS_PENDING;

    private UserConstant() {
    }
}
