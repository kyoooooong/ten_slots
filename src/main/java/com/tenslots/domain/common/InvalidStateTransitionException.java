package com.tenslots.domain.common;

public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(String current, String target) {
        super(current + " → " + target + " 전이 불가");
    }
}
