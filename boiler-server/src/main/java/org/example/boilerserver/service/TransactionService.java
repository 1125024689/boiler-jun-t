package org.example.boilerserver.service;

import org.example.boilerpojo.BookPostDTO;
import org.example.boilerpojo.CancelBookingDTO;
import org.example.boilerpojo.CompleteTransactionDTO;
import org.example.boilerpojo.TransactionVO;
import org.example.boilerpojo.UpdateLogisticsDTO;

import java.util.List;

public interface TransactionService {
    TransactionVO bookPost(BookPostDTO dto);

    TransactionVO cancelBooking(CancelBookingDTO dto);

    TransactionVO completeTransaction(CompleteTransactionDTO dto);

    TransactionVO updateLogistics(UpdateLogisticsDTO dto);

    TransactionVO getTransaction(String transactionId);

    List<TransactionVO> listByBuyer(String buyerId);

    List<TransactionVO> listBySeller(String sellerId);
}
