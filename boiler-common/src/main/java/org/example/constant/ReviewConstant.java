package org.example.constant;

public final class ReviewConstant {
    // 评分范围 rating range
    public static final int MIN_RATING = 1;
    public static final int MAX_RATING = 5;

    // 好评阈值：4-5星为好评，1-2星为差评
    public static final int POSITIVE_RATING_THRESHOLD = 4;

    // 信用分规则：好评 +3 分，差评 -5 分
    public static final int CREDIT_SCORE_POSITIVE = 3;
    public static final int CREDIT_SCORE_NEGATIVE = -5;

    private ReviewConstant() {
    }
}
