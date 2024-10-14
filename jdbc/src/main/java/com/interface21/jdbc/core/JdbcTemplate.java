package com.interface21.jdbc.core;

import com.interface21.dao.DataAccessException;
import com.interface21.jdbc.JdbcException;
import com.interface21.jdbc.core.extractor.ExtractionRule;
import com.interface21.jdbc.core.extractor.ExtractorMaker;
import com.interface21.jdbc.core.extractor.ManualExtractor;
import com.interface21.jdbc.core.mapper.ObjectMappedStatement;
import com.interface21.jdbc.core.extractor.ResultSetExtractor;
import com.interface21.jdbc.core.extractor.ReflectiveExtractor;
import com.interface21.jdbc.core.mapper.PreparedStatementMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

public class JdbcTemplate {

    private static final Logger log = LoggerFactory.getLogger(JdbcTemplate.class);
    private static final String MULTIPLE_DATA_ERROR = "두 개 이상의 데이터가 조회되었습니다.";

    private final DataSource dataSource;

    public JdbcTemplate(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    @Nullable
    public <T> T queryOne(Class<T> clazz, String sql, Object... params) {
        return doQueryOne(query(clazz, sql, params));
    }


    @Nullable
    public <T> T queryOne(ExtractionRule<T> extractionRule, String sql, Object... params) {
        return doQueryOne(query(extractionRule, sql, params));
    }

    private <T> T doQueryOne(List<T> result) {
        validateResultLessOrEqualThanOne(result);
        return result.isEmpty() ? null : result.getFirst();
    }

    private <T> void validateResultLessOrEqualThanOne(List<T> result) {
        if (result.size() > 1) {
            throw new DataAccessException(MULTIPLE_DATA_ERROR);
        }
    }

    @Nonnull
    public <T> List<T> query(Class<T> clazz, String sql, Object... params) {
        return doQuery(sql, params, resultSet -> new ReflectiveExtractor<>(resultSet, clazz));
    }

    @Nonnull
    public <T> List<T> query(ExtractionRule<T> extractionRule, String sql, Object... params) {
        return doQuery(sql, params, resultSet -> new ManualExtractor<>(resultSet, extractionRule));
    }

    private <T> List<T> doQuery(String sql, Object[] params, ExtractorMaker<T> extractorMaker) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             PreparedStatementMapper statement = new ObjectMappedStatement(preparedStatement, params);
             ResultSet resultSet = statement.executeQuery();
             ResultSetExtractor<T> extractor = extractorMaker.from(resultSet)) {
            return extractor.extract();
        } catch (SQLException e) {
            throw new JdbcException(e);
        }
    }

    public int update(String sql, Object... params) {
        log.info("update sql = {}", sql);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             PreparedStatementMapper objectMapper = new ObjectMappedStatement(preparedStatement, params)) {
            return objectMapper.executeUpdate();
        } catch (SQLException e) {
            throw new JdbcException(e);
        }
    }
}
