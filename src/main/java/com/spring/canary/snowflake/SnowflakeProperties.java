package com.spring.canary.snowflake;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "snowflake")
class SnowflakeProperties {

    // getters/setters
    /**
     * UTC epoch in millis (default: 2025-09-22T00:00:00Z)
     */
    private long epochMillis = 1758499200000L;

    /**
     * Node ID (0..1023). 미지정 시 autoDetectNodeId=true면 추론.
     */
    private Long nodeId;

    /**
     * 시계 역행 허용 범위(ms). 작을수록 보수적.
     */
    private long clockSkewToleranceMillis = 10L;

    /**
     * true면 nodeId 미지정 시 MAC 해시 등으로 추론.
     */
    private boolean autoDetectNodeId = true;

}
