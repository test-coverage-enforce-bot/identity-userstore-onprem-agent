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
import org.wso2.carbon.identity.user.store.common.model.AccessToken;
import org.wso2.carbon.identity.user.store.outbound.exception.WSUserStoreException;
import org.wso2.carbon.identity.user.store.outbound.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TokenMgtDao {

    public String getAccessToken(String tenantDomain, String domain, String status) throws WSUserStoreException {
        Connection connection = DatabaseUtil.getInstance().getDBConnection();
        PreparedStatement insertTokenPrepStmt = null;
        ResultSet resultSet = null;
        try {
            insertTokenPrepStmt = connection.prepareStatement(SQLQueries.ACCESS_TOKEN_GET);
            insertTokenPrepStmt.setString(1, tenantDomain);
            insertTokenPrepStmt.setString(2, domain);
            insertTokenPrepStmt.setString(3, status);
            resultSet = insertTokenPrepStmt.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("UM_TOKEN");
            }
        } catch (SQLException e) {
            throw new WSUserStoreException("Error occurred while reading agent connection for tenant " + tenantDomain,
                    e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet, insertTokenPrepStmt);
        }
        return null;
    }

    /**
     * Inserting access token
     * @param token access token
     * @return result of the insertion
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
            insertTokenPrepStmt.setString(4, token.getDomain());
            insertTokenPrepStmt.executeUpdate();
            connection.commit();
            return true;
        } catch (SQLException e) {
            throw new WSUserStoreException("Error occurred while persisting access token", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, insertTokenPrepStmt);
        }
    }

    /**
     * Update access token status for particular tenant and user store
     * @param tenantDomain Tenant domain
     * @param domain User store domain name
     * @param status Status (Ex. A=Active, I=Inactive)
     * @return result
     * @throws WSUserStoreException
     */
    public boolean updateAccessTokenStatus(String tenantDomain, String domain, String status)
            throws WSUserStoreException {
        Connection connection = DatabaseUtil.getInstance().getDBConnection();
        PreparedStatement insertTokenPrepStmt = null;
        try {
            insertTokenPrepStmt = connection.prepareStatement(SQLQueries.ACCESS_TOKEN_STATUS_UPDATE);
            insertTokenPrepStmt.setString(1, status);
            insertTokenPrepStmt.setString(2, tenantDomain);
            insertTokenPrepStmt.setString(3, domain);
            insertTokenPrepStmt.executeUpdate();
            connection.commit();
            return true;
        } catch (SQLException e) {
            throw new WSUserStoreException("Error occurred while deactivating access for domain: " + domain, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, insertTokenPrepStmt);
        }
    }

    /**
     * Update access token status
     * @param accessToken Access token
     * @param status Status of the access token
     * @return result of the update operation
     * @throws WSUserStoreException
     */
    public boolean updateAccessTokenStatus(String accessToken, String status) throws WSUserStoreException {

        Connection connection = DatabaseUtil.getInstance().getDBConnection();
        PreparedStatement insertTokenPrepStmt = null;
        try {
            insertTokenPrepStmt = connection.prepareStatement(SQLQueries.ACCESS_TOKEN_STATUS_UPDATE_BY_TOKEN);
            insertTokenPrepStmt.setString(1, status);
            insertTokenPrepStmt.setString(2, accessToken);
            insertTokenPrepStmt.executeUpdate();
            connection.commit();
            return true;
        } catch (SQLException e) {
            throw new WSUserStoreException("Error occurred while updating access token status to:" + status, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, insertTokenPrepStmt);
        }
    }
}
