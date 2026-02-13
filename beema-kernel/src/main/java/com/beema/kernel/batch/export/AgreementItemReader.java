package com.beema.kernel.batch.export;

import com.beema.kernel.domain.agreement.Agreement;
import com.beema.kernel.domain.agreement.AgreementStatus;
import com.beema.kernel.domain.base.TemporalKey;
import com.beema.kernel.domain.metadata.MarketContext;
import com.beema.kernel.util.JsonbConverter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Reads current agreements from the database for a specific tenant.
 * Uses a JDBC cursor for memory-efficient streaming of large datasets.
 */
public class AgreementItemReader extends JdbcCursorItemReader<Agreement> {

    private static final String SQL =
            "SELECT id, valid_from, transaction_time, valid_to, is_current, " +
            "agreement_number, agreement_type_code, market_context, status, " +
            "attributes, data_residency_region, tenant_id, " +
            "created_by, updated_by, version, created_at, updated_at " +
            "FROM agreements WHERE is_current = true AND tenant_id = ?";

    public AgreementItemReader(DataSource dataSource, String tenantId) {
        setName("agreementItemReader");
        setDataSource(dataSource);
        setSql(SQL);
        setPreparedStatementSetter(ps -> ps.setString(1, tenantId));
        setRowMapper(new AgreementRowMapper());
        setFetchSize(1000);
    }

    private static class AgreementRowMapper implements RowMapper<Agreement> {

        private final JsonbConverter jsonbConverter = new JsonbConverter(new com.fasterxml.jackson.databind.ObjectMapper());

        @Override
        public Agreement mapRow(ResultSet rs, int rowNum) throws SQLException {
            Agreement agreement = new Agreement();

            TemporalKey key = new TemporalKey(
                    UUID.fromString(rs.getString("id")),
                    rs.getObject("valid_from", OffsetDateTime.class),
                    rs.getObject("transaction_time", OffsetDateTime.class)
            );
            agreement.setTemporalKey(key);
            agreement.setValidTo(rs.getObject("valid_to", OffsetDateTime.class));
            agreement.setIsCurrent(rs.getBoolean("is_current"));
            agreement.setAgreementNumber(rs.getString("agreement_number"));
            agreement.setAgreementTypeCode(rs.getString("agreement_type_code"));
            agreement.setMarketContext(MarketContext.valueOf(rs.getString("market_context")));
            agreement.setStatus(AgreementStatus.valueOf(rs.getString("status")));

            String attributesJson = rs.getString("attributes");
            if (attributesJson != null) {
                Map<String, Object> attrs = jsonbConverter.convertToEntityAttribute(attributesJson);
                agreement.setAttributes(attrs);
            }

            agreement.setDataResidencyRegion(rs.getString("data_residency_region"));
            agreement.setTenantId(rs.getString("tenant_id"));
            agreement.setCreatedBy(rs.getString("created_by"));
            agreement.setUpdatedBy(rs.getString("updated_by"));
            agreement.setVersion(rs.getLong("version"));
            agreement.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
            agreement.setUpdatedAt(rs.getObject("updated_at", OffsetDateTime.class));

            return agreement;
        }
    }
}
