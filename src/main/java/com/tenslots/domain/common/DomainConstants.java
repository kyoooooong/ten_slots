package com.tenslots.domain.common;

/**
 * 도메인 공통 상수.
 * JPA @Column 어노테이션에 사용되는 값은 컴파일 타임 상수(static final)여야 합니다.
 */
public final class DomainConstants {

    private DomainConstants() {}

    /** TSID 문자열 길이 (13자) */
    public static final int TSID_LENGTH = 13;

    /** 일반 문자열 컬럼 최대 길이 */
    public static final int VARCHAR_LENGTH = 255;

    /** 금액 컬럼 precision (최대 12자리 정수부 + 소수부) */
    public static final int PRICE_PRECISION = 12;

    /** 금액 컬럼 scale (소수점 2자리) */
    public static final int PRICE_SCALE = 2;
}
