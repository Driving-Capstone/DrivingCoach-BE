package com.drivingcoach.backend.domain.setting.repository;

import com.drivingcoach.backend.domain.setting.domain.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * ✅ SettingRepository
 *
 * 역할
 *  - 사용자 환경설정(Setting) 엔티티에 대한 표준 CRUD를 제공합니다.
 *
 * 자주 쓰는 패턴
 *  - findByUserId(Long userId)
 *      → "내 설정" 화면/엔드포인트에서 가장 자주 사용됩니다.
 *  - existsByUserId(Long userId)
 *      → 회원 가입 시 기본 Setting 생성 여부를 판단하거나, 마이그레이션/정합성 점검에 유용합니다.
 *
 * 참고
 *  - User : Setting = 1 : 1 관계이므로, userId로 단건 Optional 조회가 정상입니다.
 *  - findByUserId(...)가 null이면, 최초 접속 시 기본값을 생성하도록 서비스에서 처리할 수 있습니다.
 */
public interface SettingRepository extends JpaRepository<Setting, Long> {

    /** 사용자 ID로 단건 설정 조회 (1:1 관계) */
    Optional<Setting> findByUserId(Long userId);

    /** 사용자 ID로 설정 존재 여부 확인 */
    boolean existsByUserId(Long userId);
}
