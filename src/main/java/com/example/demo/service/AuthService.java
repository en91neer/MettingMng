package com.example.demo.service;

import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.AuthCodeResponseDto;
import com.example.demo.dto.AuthVerifyResponseDto;
import com.example.demo.dto.SignupRequestDto;
import com.example.demo.entity.User;
import com.example.demo.repository.UserActionLogRepository;
import com.example.demo.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final String ADMIN_EMAIL = "en91neer@naver.com";
    private static final String ROLE_FREE_USER = "FREE_USER";
    private static final String ROLE_SUPER_USER = "SUPER_USER";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String ACTION_ANALYZE = "ANALYZE";
    private static final int FREE_ANALYZE_LIMIT = 3;
    public static final String PREMIUM_UPGRADE_MESSAGE =
            "무료 체험 횟수를 모두 사용하셨습니다.\n"
            + "더 많은 녹음 및 분석 기능을 이용하려면 프리미엄 플랜을 구독해주세요.";
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^0\\d{8,10}$");

    private final UserRepository userRepository;
    private final UserActionLogRepository userActionLogRepository;
    private final MailService mailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${auth.mail-send-enabled:false}")
    private boolean mailSendEnabled;

    @Transactional
    public AuthCodeResponseDto requestCode(String email, String requestIp) {
        String normalizedEmail = normalizeEmail(email);
        String code = String.format("%06d", secureRandom.nextInt(1_000_000));

        User user =
                userRepository
                        .findByEmail(normalizedEmail)
                        .orElseThrow(() -> new RuntimeException("등록된 사용자가 없습니다."));

        ensureAdminUser(user);
        validateLoginAllowed(user);

        user.setLoginCode(code);
        user.setLoginCodeExpiresAt(LocalDateTime.now().plusMinutes(10));
        user.setRequestIp(requestIp);

        userRepository.save(user);

        if (mailSendEnabled) {
            mailService.sendLoginCode(normalizedEmail, code);
            log.info("로그인 인증코드 이메일 발송 email={}, ip={}", normalizedEmail, requestIp);

            return new AuthCodeResponseDto(normalizedEmail, null);
        }

        log.info("개발자용 로그인 인증코드 생성 email={}, ip={}, code={}", normalizedEmail, requestIp, code);

        return new AuthCodeResponseDto(normalizedEmail, code);
    }

    @Transactional
    public void signup(SignupRequestDto dto) {
        String normalizedEmail = normalizeEmail(dto.getEmail());

        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new RuntimeException("이름을 입력해주세요.");
        }

        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new RuntimeException("올바른 이메일 형식으로 입력해주세요.");
        }

        if (dto.getPhoneNumber() == null || dto.getPhoneNumber().isBlank()) {
            throw new RuntimeException("전화번호를 입력해주세요.");
        }

        String normalizedPhoneNumber = normalizePhoneNumber(dto.getPhoneNumber());
        if (!PHONE_PATTERN.matcher(normalizedPhoneNumber).matches()) {
            throw new RuntimeException("올바른 전화번호 형식으로 입력해주세요.");
        }

        if (!Boolean.TRUE.equals(dto.getAgreeTerms())) {
            throw new RuntimeException("이용약관에 동의해주세요.");
        }

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new RuntimeException("이미 가입된 이메일입니다.");
        }

        User user =
                User.builder()
                        .email(normalizedEmail)
                        .name(dto.getName().trim())
                        .phoneNumber(normalizedPhoneNumber)
                        .roleCode(isAdminEmail(normalizedEmail) ? ROLE_SUPER_USER : ROLE_FREE_USER)
                        .statusCode(STATUS_ACTIVE)
                        .termsAgreedAt(LocalDateTime.now())
                        .build();

        userRepository.save(user);
    }

    @Transactional
    public AuthVerifyResponseDto verifyCode(
            String email,
            String code,
            boolean rememberOneDay,
            String requestIp
    ) {
        String normalizedEmail = normalizeEmail(email);

        User user =
                userRepository
                        .findByEmail(normalizedEmail)
                        .orElseThrow(() -> new RuntimeException("인증코드를 먼저 요청해주세요."));

        if (user.getLoginCode() == null || !user.getLoginCode().equals(code)) {
            throw new RuntimeException("인증코드가 올바르지 않습니다.");
        }

        if (
                user.getLoginCodeExpiresAt() == null
                || user.getLoginCodeExpiresAt().isBefore(LocalDateTime.now())
        ) {
            throw new RuntimeException("인증코드가 만료되었습니다.");
        }

        LocalDateTime expiresAt = rememberOneDay
                ? LocalDateTime.now().plusDays(1)
                : LocalDateTime.now().plusHours(2);

        user.setSessionToken(UUID.randomUUID().toString());
        user.setSessionExpiresAt(expiresAt);
        user.setRequestIp(requestIp);
        user.setLoginCode(null);
        user.setLoginCodeExpiresAt(null);

        User savedUser = userRepository.save(user);

        return new AuthVerifyResponseDto(
                savedUser.getEmail(),
                savedUser.getSessionToken(),
                savedUser.getSessionExpiresAt(),
                savedUser.getRoleCode(),
                savedUser.getStatusCode()
        );
    }

    public void validateSession(String email, String token) {
        if (email == null || token == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }

        User user =
                userRepository
                        .findBySessionToken(token)
                        .orElseThrow(() -> new RuntimeException("로그인이 필요합니다."));

        if (!user.getEmail().equalsIgnoreCase(email)) {
            throw new RuntimeException("로그인 정보가 올바르지 않습니다.");
        }

        if (user.getSessionExpiresAt() == null || user.getSessionExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("로그인이 만료되었습니다.");
        }

        validateLoginAllowed(user);
    }

    public void validateAnalyzeAllowed(String email) {
        User user =
                userRepository
                        .findByEmail(normalizeEmail(email))
                        .orElseThrow(() -> new RuntimeException("로그인이 필요합니다."));

        validateLoginAllowed(user);

        if (!ROLE_FREE_USER.equals(user.getRoleCode())) {
            return;
        }

        long analyzeCount = getAnalyzeCount(user.getEmail());

        if (analyzeCount >= FREE_ANALYZE_LIMIT) {
            throw new RuntimeException(PREMIUM_UPGRADE_MESSAGE);
        }
    }

    public Map<String, Object> getAnalyzeUsage(String email) {
        User user =
                userRepository
                        .findByEmail(normalizeEmail(email))
                        .orElseThrow(() -> new RuntimeException("로그인이 필요합니다."));

        validateLoginAllowed(user);

        if (!ROLE_FREE_USER.equals(user.getRoleCode())) {
            return Map.of(
                    "allowed", true,
                    "roleCode", user.getRoleCode(),
                    "analyzeCount", 0,
                    "limit", -1,
                    "remaining", -1,
                    "message", ""
            );
        }

        long analyzeCount = getAnalyzeCount(user.getEmail());
        long remaining = Math.max(0, FREE_ANALYZE_LIMIT - analyzeCount);
        boolean allowed = analyzeCount < FREE_ANALYZE_LIMIT;

        return Map.of(
                "allowed", allowed,
                "roleCode", user.getRoleCode(),
                "analyzeCount", analyzeCount,
                "limit", FREE_ANALYZE_LIMIT,
                "remaining", remaining,
                "message", allowed ? "" : PREMIUM_UPGRADE_MESSAGE
        );
    }

    private long getAnalyzeCount(String email) {
        return userActionLogRepository.countByLoginEmailIgnoreCaseAndActionType(
                email,
                ACTION_ANALYZE
        );
    }

    public void validateAdmin(String email, String token) {
        validateSession(email, token);

        User user =
                userRepository
                        .findByEmail(normalizeEmail(email))
                        .orElseThrow(() -> new RuntimeException("로그인이 필요합니다."));

        if (!ROLE_SUPER_USER.equals(user.getRoleCode())) {
            throw new RuntimeException("관리자 권한이 필요합니다.");
        }
    }

    @Transactional
    public void logout(String email, String token) {
        validateSession(email, token);

        User user =
                userRepository
                        .findBySessionToken(token)
                        .orElseThrow(() -> new RuntimeException("로그인이 필요합니다."));

        user.setSessionToken(null);
        user.setSessionExpiresAt(null);

        userRepository.save(user);
    }

    public boolean isSuperUserEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        String normalizedEmail = email.trim().toLowerCase();

        if (isAdminEmail(normalizedEmail)) {
            return true;
        }

        return userRepository
                .findByEmail(normalizedEmail)
                .map(user -> ROLE_SUPER_USER.equals(user.getRoleCode()))
                .orElse(false);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("이메일을 입력해주세요.");
        }

        return email.trim().toLowerCase();
    }

    private String normalizePhoneNumber(String phoneNumber) {
        return phoneNumber.trim().replaceAll("[\\s-]", "");
    }

    private void validateLoginAllowed(User user) {
        if (!STATUS_ACTIVE.equals(user.getStatusCode())) {
            throw new RuntimeException("관리자 승인 후 로그인이 가능합니다.");
        }
    }

    private void ensureAdminUser(User user) {
        if (!isAdminEmail(user.getEmail())) {
            return;
        }

        user.setRoleCode(ROLE_SUPER_USER);
        user.setStatusCode(STATUS_ACTIVE);
    }

    private boolean isAdminEmail(String email) {
        return ADMIN_EMAIL.equalsIgnoreCase(email);
    }

}
