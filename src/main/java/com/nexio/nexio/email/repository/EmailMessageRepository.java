package com.nexio.nexio.email.repository;

import com.nexio.nexio.email.model.EmailMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailMessageRepository extends JpaRepository<EmailMessage,Long> {
    List<EmailMessage> findByUserIdOrderByReceivedAtDesc(Long userId);

    List<EmailMessage> findByUserIdAndJobRelatedTrueOrderByReceivedAtDesc(Long userId);

    boolean existsByGmailMessageId(String gmailMessageId);
}
