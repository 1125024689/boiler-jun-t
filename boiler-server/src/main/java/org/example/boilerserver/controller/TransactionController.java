package org.example.boilerserver.controller;

import org.example.boilercommon.Result;
import org.example.boilerpojo.BookPostDTO;
import org.example.boilerpojo.CancelBookingDTO;
import org.example.boilerpojo.CompleteTransactionDTO;
import org.example.boilerpojo.TransactionVO;
import org.example.boilerpojo.UpdateLogisticsDTO;
import org.example.boilerserver.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/transaction")
public class TransactionController {
    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/book")
    public Result<TransactionVO> bookPost(@RequestBody BookPostDTO dto) {
        log.info("预约帖子: buyerId={}, postId={}", dto.getBuyerId(), dto.getPostId());
        return Result.success(transactionService.bookPost(dto));
    }

    @PutMapping("/cancel-booking")
    public Result<TransactionVO> cancelBooking(@RequestBody CancelBookingDTO dto) {
        log.info("取消预约: transactionId={}, buyerId={}", dto.getTransactionId(), dto.getBuyerId());
        return Result.success(transactionService.cancelBooking(dto));
    }

    @PutMapping("/complete")
    public Result<TransactionVO> completeTransaction(@RequestBody CompleteTransactionDTO dto) {
        log.info("完成交易: transactionId={}, sellerId={}", dto.getTransactionId(), dto.getSellerId());
        return Result.success(transactionService.completeTransaction(dto));
    }

    @PutMapping("/logistics")
    public Result<TransactionVO> updateLogistics(@RequestBody UpdateLogisticsDTO dto) {
        log.info("更新物流: transactionId={}, sellerId={}", dto.getTransactionId(), dto.getSellerId());
        return Result.success(transactionService.updateLogistics(dto));
    }

    @GetMapping("/{transactionId}")
    public Result<TransactionVO> getTransaction(@PathVariable String transactionId) {
        log.info("查询交易详情: transactionId={}", transactionId);
        return Result.success(transactionService.getTransaction(transactionId));
    }

    @GetMapping("/buyer/{buyerId}")
    public Result<List<TransactionVO>> listByBuyer(@PathVariable String buyerId) {
        log.info("查询买家交易列表: buyerId={}", buyerId);
        return Result.success(transactionService.listByBuyer(buyerId));
    }

    @GetMapping("/seller/{sellerId}")
    public Result<List<TransactionVO>> listBySeller(@PathVariable String sellerId) {
        log.info("查询卖家交易列表: sellerId={}", sellerId);
        return Result.success(transactionService.listBySeller(sellerId));
    }
}
