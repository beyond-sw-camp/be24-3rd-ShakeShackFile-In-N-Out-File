package com.example.WaffleBear.group.model.entity;

import com.example.WaffleBear.group.model.enums.InviteStatus;
import com.example.WaffleBear.group.model.enums.InviteType;
import com.example.WaffleBear.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "relationship_invite",
        indexes = {
                @Index(name = "idx_relationship_invite_from_user", columnList = "from_user_id"),
                @Index(name = "idx_relationship_invite_to_user", columnList = "to_user_id"),
                @Index(name = "idx_relationship_invite_email", columnList = "email")
        }
)
public class RelationshipInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id", nullable = false)
    private User fromUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id")
    private User toUser;

    @Column(length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private InviteType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private InviteStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void accept() {
        this.status = InviteStatus.ACCEPTED;
    }

    public void reject() {
        this.status = InviteStatus.REJECTED;
    }

    public void bindToUser(User user) {
        this.toUser = user;
        if (user != null && (this.email == null || this.email.isBlank())) {
            this.email = user.getEmail();
        }
    }
}
