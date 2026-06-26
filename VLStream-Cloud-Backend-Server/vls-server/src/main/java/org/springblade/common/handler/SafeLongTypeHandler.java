package org.springblade.common.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Safe Long type handler (SafeLongTypeHandler)
 * Adapt to cases where non-pure numeric strings such as UUIDs exist in the database.
 * When the read value is a non-numeric string, null is returned to avoid throwing a NumberFormatException that causes interface errors.
 */
@Component
@MappedTypes({Long.class, long.class})
public class SafeLongTypeHandler extends BaseTypeHandler<Long> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Long parameter, JdbcType jdbcType) throws SQLException {
        ps.setLong(i, parameter);
    }

    /**
     * Convert string value to Long
     *
     * @param val string value
     * @return converted Long value, or null if conversion fails
     */
    private Long toLong(String val) {
        if (val == null || val.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            // If non-pure digits are encountered (such as UUID string "0ffc6e35-..."), catch exception and safely return null
            return null;
        }
    }

    @Override
    public Long getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String val = rs.getString(columnName);
        return toLong(val);
    }

    @Override
    public Long getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String val = rs.getString(columnIndex);
        return toLong(val);
    }

    @Override
    public Long getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String val = cs.getString(columnIndex);
        return toLong(val);
    }
}
