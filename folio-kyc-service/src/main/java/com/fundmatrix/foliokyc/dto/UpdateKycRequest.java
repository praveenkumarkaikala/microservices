package com.fundmatrix.foliokyc.dto;

import com.fundmatrix.foliokyc.domain.enums.KycType;

public record UpdateKycRequest(String documentRef,String documentType,KycType kycType) {

}
