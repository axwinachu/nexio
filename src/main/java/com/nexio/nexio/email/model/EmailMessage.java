package com.nexio.nexio.email.model;

import com.nexio.nexio.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Original Gmail message ID — prevents duplicate syncs
    @Column(nullable = false, unique = true)
    private String gmailMessageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String sender;
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    private LocalDateTime receivedAt;


    @Column(nullable = false)
    private boolean jobRelated;
}
