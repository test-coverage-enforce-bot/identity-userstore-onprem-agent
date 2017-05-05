/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.identity.user.store.outbound.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.user.store.outbound.exception.WSUserStoreException;

import java.sql.Connection;
import java.sql.SQLException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class DatabaseUtil {

    private static Log LOGGER = LogFactory.getLog(DatabaseUtil.class);
    private DataSource dataSource;
    private static final String DATA_SOURCE_NAME = "jdbc/TokenDB";
    private static volatile DatabaseUtil instance = new DatabaseUtil();

    private DatabaseUtil() {
        initDataSource();
    }

    public static DatabaseUtil getInstance() {
        return instance;
    }

    /**
     * Initialize datasource
     */
    private void initDataSource() {

        try {
            Context ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup(DATA_SOURCE_NAME);
        } catch (NamingException e) {
            String errorMsg = "Error when looking up the Identity Data Source.";
            LOGGER.error(errorMsg, e);
        }
    }

    /**
     * Get database connection
     * @return database connection
     * @throws WSUserStoreException
     */
    public Connection getDBConnection() throws WSUserStoreException {
        try {
            if (dataSource == null) {
                throw new WSUserStoreException("Error occurred while getting connection. Datasource is null");
            }
            Connection dbConnection = dataSource.getConnection();
            dbConnection.setAutoCommit(false);
            dbConnection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            return dbConnection;
        } catch (SQLException e) {
            String errMsg = "Error when getting a database connection object from the Identity data source.";
            throw new WSUserStoreException(errMsg, e);
        }
    }
}
