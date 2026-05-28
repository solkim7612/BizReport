package com.bizreport.core.repository.data;

import com.bizreport.core.entity.data.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DataJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public void insert(List<Data> dataList) {
        String sql = "INSERT INTO data (b_id, trans_dt, data_type, data_method, is_e, is_mod, vendor_id, net_value, vat_value, total_price, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";

        int batchSize = 1000;
        List<Data> chunk = new ArrayList<>();

        for (int i = 0; i < dataList.size(); i++) {
            chunk.add(dataList.get(i));

            if ((i + 1) % batchSize == 0) {
                batch(sql, chunk);
                chunk.clear();
            }
        }

        if (!chunk.isEmpty()) {
            batch(sql, chunk);
        }
    }

    private void batch(String sql, List<Data> chunk) {
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Data data = chunk.get(i);

                ps.setString(1, data.getUser().getId());
                ps.setObject(2, data.getTransDt());
                ps.setString(3, data.getType().name());
                ps.setString(4, data.getMethod().name());
                ps.setBoolean(5, data.isE());
                ps.setBoolean(6, data.isMod());
                ps.setString(7, data.getVendorId() != null ? data.getVendorId() : "0000000000");
                ps.setBigDecimal(8, data.getNetValue());
                ps.setBigDecimal(9, data.getVatValue());
                ps.setBigDecimal(10, data.getTotalPrice());
            }

            @Override
            public int getBatchSize() {
                return chunk.size();
            }
        });
    }
}