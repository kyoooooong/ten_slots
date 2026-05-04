package com.tenslots.domain.common;

// 상태머신 전이 규칙 위반 — 정상 플로우에서는 발생하지 않아야 하는 프로그래밍 오류
public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(String current, String target) {
        super(current + " → " + target + " 전이 불가");
    }
}
