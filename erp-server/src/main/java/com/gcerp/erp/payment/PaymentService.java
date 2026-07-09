package com.gcerp.erp.payment;

import com.gcerp.erp.contract.ContractService;
import com.gcerp.erp.workflow.OrderStatusConst;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRecordMapper paymentRecordMapper;
    private final ContractService contractService;

    @Transactional
    public PaymentRecord checkPayment(PaymentCheckRequest req) {
        PaymentRecord record = new PaymentRecord();
        record.setContractId(req.getContractId());
        record.setAmount(req.getAmount());
        record.setPayStatus(req.getPayStatus());
        record.setPayChannel(req.getPayChannel());
        record.setCheckedBy(req.getOperator());
        record.setCheckedAt(LocalDateTime.now());
        record.setCustomFields(req.getCustomFields());
        record.setCustomText1(req.getCustomText1());
        record.setCustomText2(req.getCustomText2());
        record.setCustomText3(req.getCustomText3());
        paymentRecordMapper.insert(record);

        if ("已到账".equals(req.getPayStatus())) {
            contractService.businessAdvance(
                    req.getContractId(),
                    OrderStatusConst.待财务确认,
                    OrderStatusConst.财务已收款,
                    req.getOperator(),
                    "财务确认已收款"
            );
        }
        return record;
    }
}
