package com.gcerp.erp.quote;

import com.gcerp.erp.contract.ContractService;
import com.gcerp.erp.workflow.OrderStatusConst;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class QuoteService {

    private final QuotationMapper quotationMapper;
    private final ContractService contractService;

    @Transactional
    public Quotation createQuote(QuoteCreateRequest req) {
        contractService.businessAdvance(
                req.getContractId(),
                OrderStatusConst.报价中,
                OrderStatusConst.待客户确认,
                req.getOperator(),
                "报价完成，待客户确认"
        );

        Quotation quotation = new Quotation();
        quotation.setContractId(req.getContractId());
        quotation.setTotalAmount(req.getTotalAmount());
        quotation.setQuoteDesc(req.getQuoteDesc());
        quotation.setCreatedBy(req.getOperator());
        quotation.setCreatedAt(LocalDateTime.now());
        quotationMapper.insert(quotation);
        return quotation;
    }
}

