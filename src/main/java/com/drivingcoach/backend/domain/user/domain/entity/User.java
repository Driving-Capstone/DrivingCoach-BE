package com.drivingcoach.backend.domain.user.domain.entity;

import com.drivingcoach.backend.domain.user.domain.constant.Gender;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@Entity
@Table(name = "users",
        indexes = {
                @Index(name = "idx_users_login_id", columnList = "login_id", unique = true)
        })
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("PK")
    private Long id;

    @Column(name = "login_id", nullable = false, unique = true, length = 50)
    @Comment("로그인 ID (unique)")
    private String loginId;

    @Column(name = "password", nullable = false, length = 255)
    @Comment("비밀번호(BCrypt 등 해시 저장)")
    private String password;

    @Column(name = "nickname", nullable = false, length = 20)
    @Comment("닉네임")
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 10)
    @Comment("성별(MALE/FEMALE/UNKNOWN)")
    private Gender gender;

    @Column(name = "birth_date")
    @Comment("생년월일")
    private LocalDate birthDate;

    @Column(name = "role", nullable = false, length = 20)
    @Comment("권한(ROLE_USER, ROLE_ADMIN 등)")
    @Builder.Default
    private String role = "ROLE_USER";

    @Column(name = "active", nullable = false)
    @Comment("활성화 여부(탈퇴/비활성화 표시)")
    @Builder.Default
    private boolean active = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    @Comment("생성 일시")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    @Comment("수정 일시")
    private LocalDateTime updatedAt;

    /* ====== 비즈니스 메서드 ====== */

    public void updateProfile(String nickname, Gender gender, LocalDate birthDate) {
        if (nickname != null && !nickname.isBlank()) this.nickname = nickname;
        if (gender != null) this.gender = gender;
        if (birthDate != null) this.birthDate = birthDate;
    }

    public void changePassword(String encodedNewPassword) {
        this.password = encodedNewPassword;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean isActive() {
        return active;
    }
}
