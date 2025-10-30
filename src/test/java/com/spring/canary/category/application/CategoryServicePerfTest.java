package com.spring.canary.category.application;


import com.spring.canary.category.application.request.CreateCategoriesRequest;
import com.spring.canary.category.infrastructure.CategoryRepository;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import javax.sql.DataSource;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@SpringBootTest
@ActiveProfiles("perf") // (아래 3) 설정 참고
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CategoryServicePerfTest {

    @Autowired
    CategoryService categoryService;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    DataSource dataSource;

    // --- 벤치마크 파라미터 ---
    static final int WARMUP_ROUNDS = 2;      // 워밍업 횟수(측정 제외)
    static final int MEASURE_ROUNDS = 5;     // 실제 측정 횟수
    static final int[] SIZES = {1_000, 5_000, 10_000}; // 한번에 생성할 카테고리 개수

    @BeforeEach
    void clean() throws Exception {
        if (isMySQL()) {
            // FK가 걸려있으면 TRUNCATE가 막히므로 잠시 비활성화
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");
            jdbcTemplate.execute("TRUNCATE TABLE category");
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
        } else {
            jdbcTemplate.execute("TRUNCATE TABLE category RESTART IDENTITY CASCADE");
        }
    }

    private boolean isMySQL() {
        try (var conn = dataSource.getConnection()) {
            String product = conn.getMetaData().getDatabaseProductName();
            return product != null && product.toLowerCase().contains("mysql");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 유틸: 랜덤 카테고리 이름 생성
    private static List<String> randomNames(int n) {
        var list = new ArrayList<String>(n);
        for (int i = 0; i < n; i++) {
            // 짧고 고유하게:  base36(랜덤 Long) + 짧은 UUID 조각
            String s = Long.toString(ThreadLocalRandom.current().nextLong(), 36)
                    + "-"
                    + UUID.randomUUID().toString().substring(0, 8);
            list.add(s);
        }
        return list;
    }

    // 유틸: 한 번 실행 시간(ns) 측정
    private static long timeNanos(Runnable r) {
        long t0 = System.nanoTime();
        r.run();
        return System.nanoTime() - t0;
    }

    @Value
    @Builder
    static class Stats {

        long minNanos;
        long maxNanos;
        double avgNanos;
        double stddevNanos;

        static Stats of(List<Long> samples) {
            long min = Long.MAX_VALUE, max = Long.MIN_VALUE;
            double sum = 0;
            for (long v : samples) {
                min = Math.min(min, v);
                max = Math.max(max, v);
                sum += v;
            }
            double avg = sum / samples.size();
            double varSum = 0;
            for (long v : samples) {
                varSum += Math.pow(v - avg, 2);
            }
            double std = Math.sqrt(varSum / samples.size());
            return Stats.builder().minNanos(min).maxNanos(max).avgNanos(avg).stddevNanos(std)
                    .build();
        }

        String pretty(String label) {
            return """
                    %s -> min=%s, max=%s, avg=%s, std=%.2f ms
                    """.formatted(
                    label,
                    Duration.ofNanos(minNanos),
                    Duration.ofNanos(maxNanos),
                    Duration.ofNanos((long) avgNanos),
                    stddevNanos / 1_000_000.0
            );
        }
    }

    private Stats runJpaOnce(int size) {
        var req = new CreateCategoriesRequest(randomNames(size));
        long nanos = timeNanos(() -> categoryService.createByJpa(req));
        return Stats.of(List.of(nanos)); // 단발 결과를 Stats 형태로 포장
    }

    private Stats runMyBatisOnce(int size) {
        var req = new CreateCategoriesRequest(randomNames(size));
        long nanos = timeNanos(() -> categoryService.createByMyBatis(req));
        return Stats.of(List.of(nanos));
    }

    private Stats runAndAggregate(boolean jpa, int size) {
        // 워밍업
        for (int i = 0; i < WARMUP_ROUNDS; i++) {
            if (jpa) {
                runJpaOnce(size);
            } else {
                runMyBatisOnce(size);
            }
            jdbcTemplate.execute("TRUNCATE TABLE category");
        }

        // 측정
        var samples = new ArrayList<Long>(MEASURE_ROUNDS);
        for (int i = 0; i < MEASURE_ROUNDS; i++) {
            long nanos = jpa
                    ? timeNanos(
                    () -> categoryService.createByJpa(new CreateCategoriesRequest(randomNames(size))))
                    : timeNanos(() -> categoryService.createByMyBatis(
                            new CreateCategoriesRequest(randomNames(size))));

            samples.add(nanos);
            // 매 라운드 후 정리
            jdbcTemplate.execute("TRUNCATE TABLE category");
        }
        return Stats.of(samples);
    }

    @Test
    @Order(1)
    @DisplayName("JPA vs MyBatis 성능 비교 (여러 사이즈)")
    void compareJpaVsMyBatis() {
        log.info("===== Category Insert Benchmark (rounds: warmup={}, measure={}) =====",
                WARMUP_ROUNDS, MEASURE_ROUNDS);

        // 연결이 실제로 살아있는지 한번 확인(실 DB에서 테스트하기 위함)
        DataSourceUtils.getConnection(dataSource);

        for (int size : SIZES) {
            var jpaStats = runAndAggregate(true, size);
            var mbStats = runAndAggregate(false, size);

            System.out.println("\n--- size = " + size + " ---");
            System.out.print(jpaStats.pretty("JPA   "));
            System.out.print(mbStats.pretty("MyBatis"));

            double jpaAvgMs = jpaStats.avgNanos / 1_000_000.0;
            double mbAvgMs = mbStats.avgNanos / 1_000_000.0;
            double ratio = jpaAvgMs / mbAvgMs;

            System.out.printf("Result: MyBatis is %.2fx %s than JPA (avg)\n",
                    ratio > 1 ? ratio : (1 / ratio),
                    ratio > 1 ? "faster" : "slower");
        }
    }
}
