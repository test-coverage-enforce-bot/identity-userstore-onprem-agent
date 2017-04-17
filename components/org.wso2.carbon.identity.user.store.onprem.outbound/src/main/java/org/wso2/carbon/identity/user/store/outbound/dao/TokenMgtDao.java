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
package org.wso2.carbon.identity.user.store.outbound.dao;

import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.user.store.outbound.exception.WSUserStoreException;
import org.wso2.carbon.identity.user.store.outbound.model.AccessToken;
import org.wso2.carbon.identity.user.store.outbound.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TokenMgtDao {

    /**
     * Inserting access token
     * @param token
     * @return
     * @throws WSUserStoreException
     */
    public boolean insertAccessToken(AccessToken token) throws WSUserStoreException {
        Connection connection = DatabaseUtil.getInstance().getDBConnection();
        PreparedStatement insertTokenPrepStmt = null;
        try {
            insertTokenPrepStmt = connection.prepareStatement(SQLQueries.ACCESS_TOKEN_INSERT);
            insertTokenPrepStmt.setString(1, token.getAccessToken());
            insertTokenPrepStmt.setString(2, token.getStatus());
            insertTokenPrepStmt.setString(3, token.getTenant());
            insertTokenPrepStmt.executeUpdate();
            connection.commit();
            return true;
        } catch (SQLException e) {
            throw new WSUserStoreException("Error occurred while persisting access token", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, insertTokenPrepStmt);
        }
    }

    public boolean deleteAccessToken(String tenantDomain) throws WSUserStoreException {
        Connection connection = DatabaseUtil.getInstance().getDBConnection();
        PreparedStatement insertTokenPrepStmt = null;
        try {
            insertTokenPrepStmt = connection.prepareStatement(SQLQueries.ACCESS_TOKEN_DELETE);
            insertTokenPrepStmt.setString(1, tenantDomain);
            insertTokenPrepStmt.executeUpdate();
            connection.commit();
            return true;
        } catch (SQLException e) {
            throw new WSUserStoreException("Error occurred while deleting access tokens in tenant : " + tenantDomain,
                    e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, insertTokenPrepStmt);
        }
    }

    public String getServerNode(String tenant) throws WSUserStoreException {
        Connection connection = DatabaseUtil.getInstance().getDBConnection();
        PreparedStatement insertTokenPrepStmt = null;
        ResultSet resultSet = null;
        try {
            insertTokenPrepStmt = connection.prepareStatement(SQLQueries.NEXT_SERVER_NODE_GET);
            insertTokenPrepStmt.setString(1, "A");
            insertTokenPrepStmt.setString(2, tenant);
            resultSet = insertTokenPrepStmt.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("UM_SERVER_NODE");
            }
        } catch (SQLException e) {
            throw new WSUserStoreException("Error occurred while persisting access token", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet, insertTokenPrepStmt);
        }
        throw new WSUserStoreException("No serve connection available");
    }

    //SELECT A.UM_SERVER_NODE FROM UM_AGENT_CONNECTIONS A,UM_ACCESS_TOKEN T WHERE A.UM_ACCESS_TOKEN = T.UM_TOKEN AND A.UM_STATUS='A' AND T.UM_TENANT='wso2.com' ORDER BY A.UM_LASTUPDATEDATE;

}
