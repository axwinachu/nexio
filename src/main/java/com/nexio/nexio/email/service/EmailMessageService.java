package com.nexio.nexio.email.service;

import com.nexio.nexio.email.model.EmailMessage;
import com.nexio.nexio.email.repository.EmailMessageRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailMessageService {
    private final EmailMessageRepository emailMessageRepository;
    public List<EmailMessage> findByUserIdOrderByReceivedAtDesc(Long userId){
        return emailMessageRepository.findByUserIdOrderByReceivedAtDesc(userId);
    }
    public List<EmailMessage> findByUserIdAndJobRelatedTrueOrderByReceivedAtDesc(Long userId){
        return emailMessageRepository.findByUserIdAndJobRelatedTrueOrderByReceivedAtDesc(userId);
    }
    @Transactional(readOnly = true)
    public boolean existsByGmailMessageId(String gmailMessageId){
        return emailMessageRepository.existsByGmailMessageId(gmailMessageId);
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EmailMessage saveInNewTransaction(EmailMessage emailMessage) {
        return emailMessageRepository.save(emailMessage);
    }
    public void save(EmailMessage emailMessage) {
        emailMessageRepository.save(emailMessage);
    }
}
