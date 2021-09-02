package com.matthey.pmm.toms.model.hibernate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.internal.StandardDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DatabaseMetaDataDialectResolutionInfoAdapter;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.jdbc.ReturningWork;

public class SequenceValueGetter {

    // For Hibernate 4
    public Long getID(final SharedSessionContractImplementor session, final String sequenceName) {
        ReturningWork<Long> maxReturningWork = new ReturningWork<Long>() {
            @Override
            public Long execute(Connection connection) throws SQLException {
                DialectResolver dialectResolver = new StandardDialectResolver();
                DatabaseMetaDataDialectResolutionInfoAdapter adapter = new DatabaseMetaDataDialectResolutionInfoAdapter(connection.getMetaData());
                Dialect dialect =  dialectResolver.resolveDialect(adapter);
                PreparedStatement preparedStatement = null;
                ResultSet resultSet = null;
                try {
                    preparedStatement = connection.prepareStatement( dialect.getSequenceNextValString(sequenceName));
                    resultSet = preparedStatement.executeQuery();
                    resultSet.next();
                    return resultSet.getLong(1);
                }catch (SQLException e) {
                    throw e;
                } finally {
                    if(preparedStatement != null) {
                        preparedStatement.close();
                    }
                    if(resultSet != null) {
                        resultSet.close();
                    }
                }

            }
        };
        Long maxRecord = session.doReturningWork(maxReturningWork);
        return maxRecord;
    }
}

