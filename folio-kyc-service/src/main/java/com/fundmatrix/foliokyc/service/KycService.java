package com.fundmatrix.foliokyc.service;

import com.fundmatrix.foliokyc.client.NotificationClient;
import com.fundmatrix.foliokyc.client.NotificationClient.NotificationRequest;
import com.fundmatrix.foliokyc.common.exception.BusinessException;
import com.fundmatrix.foliokyc.common.exception.ResourceNotFoundException;
import com.fundmatrix.foliokyc.domain.InvestorFolio;
import com.fundmatrix.foliokyc.domain.KycRecord;
import com.fundmatrix.foliokyc.domain.enums.KycStatus;
import com.fundmatrix.foliokyc.domain.enums.Role;
import com.fundmatrix.foliokyc.dto.FolioDto;
import com.fundmatrix.foliokyc.dto.KycRecordDto;
import com.fundmatrix.foliokyc.dto.KycStatusDto;
import com.fundmatrix.foliokyc.dto.SubmitKycRequest;
import com.fundmatrix.foliokyc.dto.UpdateKycRequest;
import com.fundmatrix.foliokyc.repository.KycRecordRepository;
import com.fundmatrix.foliokyc.security.CurrentUserService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;


@Service
public class KycService {

    private final KycRecordRepository kycRepository;
    private final NotificationClient notificationClient;
    private final AuditService auditService;
    private final CurrentUserService currentUser;
    private final Mapper mapper;
    private FolioService folioService;

    public KycService(KycRecordRepository kycRepository, NotificationClient notificationClient,
                      AuditService auditService, CurrentUserService currentUser, Mapper mapper,FolioService folioService) {
        this.kycRepository = kycRepository;
        this.notificationClient = notificationClient;
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.mapper = mapper;
        this.folioService=folioService;
    }

   
    @Transactional
    public KycRecordDto createKyc(SubmitKycRequest req) {
        if (currentUser.getRole() != Role.INVESTOR) {
            throw new BusinessException("Only investors can submit KYC");
        }
        Long investorId = currentUser.getId();
        KycRecord r = kycRepository.findByInvestorId(investorId);
        if(r!=null)
        {
        	throw new BusinessException("KYC Record Already Exists");
        }
        
        KycRecord record = KycRecord.builder()
                .investorId(investorId)
                .kycType(req.kycType())
                .documentType(req.documentType())
                .documentRef(req.documentRef())
                .kycStatus(KycStatus.PENDING)
                .build();
        record = kycRepository.save(record);
        auditService.record("KYC_SUBMIT", "KycRecord", record.getId(),
                "KYC submitted by investor " + investorId);
        notificationClient.notify(new NotificationRequest(investorId, "KYC",
                "Your KYC has been submitted and is pending verification"));
        return mapper.toKycDto(record);
    }

    @Transactional(readOnly = true)
    public List<KycRecordDto> getkycList(KycStatus status) {
        List<KycRecord> records = (status == null)
                ? kycRepository.findAll() : kycRepository.findByKycStatus(status);
        return records.stream().map((record)->{return toDto(record);}).toList();
    }

    @Transactional(readOnly = true)
    public KycRecordDto kycForInvestor(Long investorId) {
    	KycRecord record = kycRepository.findByInvestorId(investorId);
        if(record==null)
        {
        	throw ResourceNotFoundException.of("KycRecord", investorId);
        }
        return mapper.toKycDto(record);
    }

    @Transactional(readOnly = true)
    public KycRecordDto mine() {
        return kycForInvestor(currentUser.getId());
    }
    
    
    @Transactional(readOnly = true)
    public KycRecordDto getKycById(long id) {
        return mapper.toKycDto(kycRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("kyc", id)));
    }

    @Transactional
    public KycRecordDto updateStatus(Long id, KycStatus status) {
        KycRecord record = kycRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("KycRecord", id));
        record.setKycStatus(status);
        if (status == KycStatus.COMPLIANT) {
            record.setVerifiedDate(LocalDate.now());
        }
        kycRepository.save(record);
        auditService.record("KYC_STATUS", "KycRecord", id, "KYC status set to " + status);
        notificationClient.notify(new NotificationRequest(record.getInvestorId(), "KYC",
                "Your KYC status is now " + status));
        return mapper.toKycDto(record);
    }

  
    @Transactional(readOnly = true)
    public KycStatusDto kycStatusFor(Long investorId) {
       
        KycRecord record = kycRepository.findByInvestorId(investorId);
        if(record==null)
        {
        	throw ResourceNotFoundException.of("KycRecord", investorId);
        }
        return new KycStatusDto(investorId,record.getKycStatus().name(), record.getKycStatus().name().equals("COMPLIANT"));
    }
    
    
    @Transactional
    public KycRecordDto renuewalKyc(UpdateKycRequest dto,Long id) {
        KycRecord record = kycRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("KycRecord", id));
        record.setDocumentRef(dto.documentRef());
        record.setDocumentType(dto.documentType());
        record.setKycType(dto.kycType());
        record.setKycStatus(KycStatus.PENDING);
        kycRepository.save(record);
        auditService.record("KYC_UPDATE", "KycRecord", id, "KYC Renuewal");
        notificationClient.notify(new NotificationRequest(record.getInvestorId(), "KYC",
                "Your KYC status is now " + record.getKycStatus()));
        return mapper.toKycDto(record);
    }
    
    
    public Boolean isKycExpired(LocalDate date)
   	{
   		if(date.plusMonths(6).isBefore(LocalDate.now()))
   		{
   			return true;
   		}
   		return false;
   	}
       
       
       
       
       @Scheduled(cron = "0 0 0 * * ?")
   	public void checkKycExpiry()
   	{
   		List<KycRecord> records=kycRepository.findAll();
   		
   			for(KycRecord record:records)
   			{
   				if(record.getVerifiedDate() != null && record.getKycStatus()==KycStatus.COMPLIANT
   						&& isKycExpired(record.getVerifiedDate()))
   				{ 	
   					record.setKycStatus(KycStatus.EXPIRED);
   					 notificationClient.notify(new NotificationRequest(record.getInvestorId(), "KYC",
   			                "Your KYC status is now " + record.getKycStatus()));
   				}
   			}
   		
   	}
       
       private KycRecordDto toDto(KycRecord record) {
           String investorName = folioService.safeInvestorName(record.getInvestorId());
           return mapper.toKycDto(record, investorName);
       }
       
}
