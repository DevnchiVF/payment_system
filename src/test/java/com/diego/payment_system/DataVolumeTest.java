package com.diego.payment_system;

import com.diego.payment_system.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class DataVolumeTest extends BaseIntegrationTest{

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void insert_100k_transactions_and_analyze_history_query(){
        jdbc.execute("""
                INSERT INTO clients(dni, name, birth_date, phone_number, email, password)
                VALUES ('11111111', 'Alice', '1990-01-01', '900000001', 'alice@test.com', 'hash'),
                                   ('22222222', 'Bob',   '1991-01-01', '900000002', 'bob@test.com',   'hash')
                """);
        jdbc.execute("""
                INSERT INTO accounts(client_id, phone_number, balance)
                VALUES (1, '900000001', 10000.00),
                                   (2, '900000002', 10000.00)
                """);
        int total = 100_000;
        int batchSize = 1000;
        for(int batch = 0; batch < total/batchSize; batch++){
            int finalBatch = batch;
            jdbc.batchUpdate("""
                    INSERT INTO transactions(source_account_id, target_account_id, amount, type, status, idempotency_key,timestamp)
                    VALUES(?, ?, ?, 'TRANSFER', 'COMPLETED', ?, ?)
                    """, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    int n = finalBatch * batchSize + i;

                    boolean aToB = n % 2 == 0;
                    ps.setLong(1, aToB ? 1 : 2);
                    ps.setLong(2, aToB ? 2 : 1);
                    ps.setBigDecimal(3, BigDecimal.valueOf(10 + (n % 990)));
                    ps.setString(4, UUID.randomUUID().toString());
                    ps.setTimestamp(5, Timestamp.valueOf(
                            LocalDateTime.now().minusDays(365).plusSeconds(n * 30L)
                    ));
                }

                @Override
                public int getBatchSize() {
                    return batchSize;
                }
            });
        }
        long count = transactionRepository.count();
        assertThat(count).isEqualTo(100_000);

        List<String> planLines = jdbc.queryForList(
                """
                EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)
                SELECT * FROM transactions
                WHERE source_account_id = 1 OR target_account_id = 1
                ORDER BY timestamp DESC
                LIMIT 20
                """, String.class);

        String plan = String.join("\n", planLines);

        System.out.println("\n========= QUERY PLAN =========\n" + plan + "\n==============================\n");

        assertThat(plan)
                .as("Query should use index scan, not sequential scan")
                .doesNotContain("Seq Scan on transactions");
    }
}
