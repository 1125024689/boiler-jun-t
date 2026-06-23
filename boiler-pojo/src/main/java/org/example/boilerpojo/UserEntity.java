package org.example.boilerpojo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserEntity{
    private String userId;
    private String username;
    private String password;
    private String phone;
    private String email;
    private String userType;
    private Integer creditScore;
    // 信用分组件
    private Integer depositPaymentScore;
    private Integer infoCompletenessScore;
    private Integer mutualRatingScore;
    private Integer transactionBehaviorScore;
    private Integer communityConductScore;
    private LocalDate registrationDate;
    private String verificationStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
