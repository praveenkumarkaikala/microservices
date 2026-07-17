package com.fundmatrix.navaccounting.domain.enums;

/**
 * Local mirror of fund-catalog-service's OptionType enum. We no longer own SchemeOption,
 * so this is parsed from the "optionType" string field of SchemeOptionDto (fetched via
 * FundCatalogClient) and snapshotted onto DividendDeclaration so business logic (reinvest
 * vs payout) doesn't need a Feign round trip on every read.
 */
public enum OptionType {
    GROWTH,
    DIVIDEND_PAYOUT,
    DIVIDEND_REINVESTMENT
}
