package com.nexio.nexio.email.model;

import com.nexio.nexio.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "email_messages",
        indexes = {
                @Index(name = "idx_gmail_message_id", columnList = "gmailMessageId"),
                @Index(name = "idx_email_user_id", columnList = "user_id")
        }
)
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
    // nullable = true here to match existing DB schema (column was created without NOT NULL)
    // Once you run a fresh migration you can set nullable = false
    @Column(unique = true)
    private String gmailMessageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String sender;
    private String subject;

    @Column(columnDefinition = "LONGTEXT")
    private String body;

    private LocalDateTime receivedAt;

    @Column(nullable = false)
    private boolean jobRelated;
}