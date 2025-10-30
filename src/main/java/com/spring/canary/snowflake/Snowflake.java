package com.spring.canary.snowflake;

import java.net.NetworkInterface;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Enumeration;
import java.util.Objects;

/**
 * 64-bit Snowflake ID generator [0][41-bit timestamp][10-bit nodeId][12-bit sequence]
 * <p>
 * - epochMillis: 서비스 기준 Epoch (밀리초) - nodeId: 0 ~ 1023 (10비트) - clockSkewToleranceMillis: 시계 역행 허용
 * 범위(작은 역행은 대기, 큰 역행은 예외)
 */
public final class Snowflake {

    // bit layout
    private static final int UNUSED_BITS = 1;       // sign bit, always 0
    private static final int EPOCH_BITS = 41;
    private static final int NODE_ID_BITS = 10;
    private static final int SEQUENCE_BITS = 12;

    // maxima
    private static final long MAX_NODE_ID = (1L << NODE_ID_BITS) - 1;     // 1023
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;   // 4095

    // shifts
    private static final int NODE_SHIFT = SEQUENCE_BITS;                               // 12
    private static final int TIMESTAMP_SHIFT = NODE_ID_BITS + SEQUENCE_BITS;           // 22

    private final long epochMillis;                 // e.g. 2025-09-22T00:00:00Z = 1758499200000L
    private final long nodeId;                      // 0..1023
    private final long clockSkewToleranceMillis;    // e.g. 5~50ms

    // 동기화된 nextId()로 보호되는 변경 가능 상태
    private long lastTimestamp;     // 가변: 최근 발급 시각
    private long sequence;          // 가변: 해당 밀리초 내 시퀀스

    public Snowflake(long epochMillis, long nodeId, long clockSkewToleranceMillis) {
        validateEpoch(epochMillis);
        validateNodeId(nodeId);
        if (clockSkewToleranceMillis < 0) {
            throw new IllegalArgumentException("clockSkewToleranceMillis must be >= 0");
        }
        this.epochMillis = epochMillis;
        this.nodeId = nodeId;
        this.clockSkewToleranceMillis = clockSkewToleranceMillis;
        this.lastTimestamp = epochMillis;
        this.sequence = 0L;
    }

    /**
     * 편의 생성자: clockSkewToleranceMillis = 10ms
     */
    public Snowflake(long epochMillis, long nodeId) {
        this(epochMillis, nodeId, 10L);
    }

    /**
     * 기본값: epoch=2025-09-22T00:00:00Z, nodeId=0, tolerance=10ms
     */
    public Snowflake() {
        this(1758499200000L, 0L, 10L);
    }

    /**
     * 스레드 안전. 매우 짧은 임계구역.
     */
    public synchronized long nextId() {
        long now = currentTime();
        if (now < lastTimestamp) {
            // 시계가 과거로 갔다. 작은 역행은 대기, 큰 역행은 실패 처리.
            long diff = lastTimestamp - now;
            if (diff <= clockSkewToleranceMillis) {
                now = waitUntil(lastTimestamp);
            } else {
                throw new IllegalStateException("Clock moved backwards by " + diff + " ms");
            }
        }

        if (now == lastTimestamp) {
            // 같은 밀리초 내에서 시퀀스 증가
            long next = (sequence + 1) & MAX_SEQUENCE;
            if (next == 0) { // overflow → 다음 밀리초까지 대기
                now = waitUntil(lastTimestamp + 1);
                sequence = 0;
            } else {
                sequence = next;
            }
        } else {
            // 새로운 밀리초 → 시퀀스 초기화
            sequence = 0;
        }

        lastTimestamp = now;

        long timestampPart = (now - epochMillis) << TIMESTAMP_SHIFT;
        long nodePart = (nodeId << NODE_SHIFT);
        return timestampPart | nodePart | sequence;
    }

    /**
     * ID에서 생성 시각(밀리초)을 역추적 (디버그/통계용)
     */
    public long extractTimeMillis(long id) {
        return (id >>> TIMESTAMP_SHIFT) + epochMillis;
    }

    /**
     * 시스템시간 래핑: 테스트에서 오버라이드 용이
     * protected -> private 변경한 상태.
     */
    private long currentTime() {
        return System.currentTimeMillis();
    }

    /**
     * 목표 시각까지 스핀 대기
     */
    private long waitUntil(long targetMillis) {
        long now = currentTime();
        while (now < targetMillis) {
            // busy-spin: 대기구간은 보통 수~수십 μs
            Thread.onSpinWait();
            now = currentTime();
        }
        return now;
    }

    private static void validateEpoch(long epochMillis) {
        // 41bit로 표현 가능한 범위를 벗어나지 않도록 최소한의 가드 (선택)
        if (epochMillis < 0) {
            throw new IllegalArgumentException("epochMillis must be >= 0");
        }
    }

    private static void validateNodeId(long nodeId) {
        if (nodeId < 0 || nodeId > MAX_NODE_ID) {
            throw new IllegalArgumentException("nodeId must be in [0, " + MAX_NODE_ID + "]");
        }
    }

    // ---- 보조: 설정이 없을 때 nodeId를 안정적으로 추론하고 싶다면 사용 ----

    /**
     * 네트워크 인터페이스 MAC 해시 기반 nodeId 추론. 설정을 못 받는 환경에서만 fallback 용도로 사용하세요.
     */
    public static long inferNodeId() {
        try {
            long acc = 0;
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface ni = ifaces.nextElement();
                byte[] mac = ni.getHardwareAddress();
                if (mac != null) {
                    for (byte b : mac) {
                        acc = (acc * 31) + (b & 0xff);
                    }
                }
            }
            if (acc == 0) {
                // MAC을 못 읽는 환경 → 보수적으로 SecureRandom fall back
                acc = new SecureRandom().nextInt() & 0x7fffffffL;
            }
            return Math.floorMod(acc, MAX_NODE_ID + 1);
        } catch (Exception e) {
            return new SecureRandom().nextInt(Math.toIntExact(MAX_NODE_ID + 1));
        }
    }

    @Override
    public String toString() {
        return "Snowflake{epoch=" + Instant.ofEpochMilli(epochMillis) +
                ", nodeId=" + nodeId +
                ", toleranceMs=" + clockSkewToleranceMillis + "}";
    }

    @Override
    public int hashCode() {
        return Objects.hash(epochMillis, nodeId, clockSkewToleranceMillis);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Snowflake other)) {
            return false;
        }
        return epochMillis == other.epochMillis
                && nodeId == other.nodeId
                && clockSkewToleranceMillis == other.clockSkewToleranceMillis;
    }
}
