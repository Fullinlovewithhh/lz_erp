package com.gcerp.erp.workflow;

import java.util.Arrays;

/**
 * 合同主流程状态。
 */
public enum ContractStatus {
    待设计,
    待报价,
    待收款,
    已收款,
    待生产,
    生产中,
    已完成;

    public static ContractStatus fromText(String text) {
        return Arrays.stream(values())
                .filter(v -> v.name().equals(text))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的状态：" + text));
    }
}
