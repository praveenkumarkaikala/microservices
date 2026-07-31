package com.fundmatrix.foliokyc.dto;


import java.math.BigDecimal;
import java.util.List;

public record HoldingPortifolio(long investorId,BigDecimal totalValue,BigDecimal totalUnrelisedGainLoss) {

}
