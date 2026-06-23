package org.example.boilerserver.util;

import org.example.boilerpojo.UserEntity;
import org.example.constant.UserConstant;

/**
 * 信用分计算工具类，供各 ServiceImpl 共用，避免重复定义
 */
public final class CreditScoreUtils {

    private CreditScoreUtils() {
    }

    /**
     * 根据各组件分数计算总信用分（上限100，下限0）
     */
    public static int calculateTotalCreditScore(UserEntity user) {
        int deposit = user.getDepositPaymentScore() == null ? 0 : user.getDepositPaymentScore();
        int info = user.getInfoCompletenessScore() == null ? 0 : user.getInfoCompletenessScore();
        int mutual = user.getMutualRatingScore() == null ? 0 : user.getMutualRatingScore();
        int behavior = user.getTransactionBehaviorScore() == null ? 0 : user.getTransactionBehaviorScore();
        int community = user.getCommunityConductScore() == null ? 0 : user.getCommunityConductScore();
        return Math.max(UserConstant.MIN_CREDIT_SCORE,
                Math.min(deposit + info + mutual + behavior + community, UserConstant.MAX_CREDIT_SCORE));
    }
}
