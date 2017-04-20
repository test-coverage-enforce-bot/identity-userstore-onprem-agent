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
import org.wso2.carbon.identity.user.store.outbound.model.AgentConnection;
import org.wso2.carbon.identity.user.store.outbound.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

    public boolean deleteAccessToken(String tenantDomain, String domain) throws WSUserStoreException {
        Connection connection = DatabaseUtil.getInstance().getDBConnection();
        PreparedStatement insertTokenPrepStmt = null;
        try {
            insertTokenPrepStmt = connection.prepareStatement(SQLQueries.ACCESS_TOKEN_DELETE);
            insertTokenPrepStmt.setString(1, domain);
            insertTokenPrepStmt.setString(2, tenantDomain);
            insertTokenPrepStmt.executeUpdate();
            connection.commit();
            return true;
        } catch (SQLException e) {
            throw new WSUserStoreException("Error occurred while deleting access for domain : " + domain,
                    e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, insertTokenPrepStmt);
        }
    }

    public List<String> getServerNodes(String tenant) throws WSUserStoreException {
        Connection connection = DatabaseUtil.getInstance().getDBConnection();
        PreparedStatement insertTokenPrepStmt = null;
        ResultSet resultSet = null;
        List<String> serverNodes = new ArrayList<>();
        try {
            insertTokenPrepStmt = connection.prepareStatement(SQLQueries.NEXT_SERVER_NODE_GET);
            insertTokenPrepStmt.setString(1, "C");
            insertTokenPrepStmt.setString(2, tenant);
            resultSet = insertTokenPrepStmt.executeQuery();
            while (resultSet.next()) {
                serverNodes.add(resultSet.getString("UM_SERVER_NODE"));
            }
        } catch (SQLException e) {
            throw new WSUserStoreException("Error occurred while persisting access token", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet, insertTokenPrepStmt);
        }
        return serverNodes;
    }

    public List<AgentConnection> getAgentConnections(String tenantDomain, String domain) throws WSUserStoreException {
        Connection connection = DatabaseUtil.getInstance().getDBConnection();
        PreparedStatement insertTokenPrepStmt = null;
        ResultSet resultSet = null;
        List<AgentConnection> agentConnections = new ArrayList<>();
        try {
            insertTokenPrepStmt = connection.prepareStatement(SQLQueries.AGENT_CONNECTIONS_GET);
            insertTokenPrepStmt.setString(1, domain);
            insertTokenPrepStmt.setString(2, tenantDomain);
            resultSet = insertTokenPrepStmt.executeQuery();
            while (resultSet.next()) {
                AgentConnection agentConnection = new AgentConnection();
                agentConnection.setStatus(resultSet.getString("UM_STATUS"));
                agentConnection.setNode(resultSet.getString("UM_NODE"));
                agentConnection.setAccessToken(resultSet.getString("UM_TOKEN"));
                agentConnections.add(agentConnection);
            }
        } catch (SQLException e) {
            throw new WSUserStoreException("Error occurred reading data", e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet, insertTokenPrepStmt);
        }
        return agentConnections;
    }

    public boolean deleteConnections(String tenantDomain, String domain) throws WSUserStoreException {

        Connection connection = DatabaseUtil.getInstance().getDBConnection();
        PreparedStatement insertTokenPrepStmt = null;
        try {
            insertTokenPrepStmt = connection.prepareStatement(SQLQueries.AGENT_CONNECTIONS_DELETE_BY_DOMAIN);
            insertTokenPrepStmt.setString(1, domain);
            insertTokenPrepStmt.setString(2, tenantDomain);
            insertTokenPrepStmt.executeUpdate();
            connection.commit();
            return true;
        } catch (SQLException e) {
            throw new WSUserStoreException("Error occurred while deleting access for domain : " + domain,
                    e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, insertTokenPrepStmt);
        }
    }

    public boolean updateConnectionStatus(String tenantDomain, String domain, String status)
            throws WSUserStoreException {

        Connection connection = DatabaseUtil.getInstance().getDBConnection();
        PreparedStatement insertTokenPrepStmt = null;
        try {
            insertTokenPrepStmt = connection.prepareStatement(SQLQueries.AGENT_CONNECTIONS_UPDATE_STATUS_BY_DOMAIN);
            insertTokenPrepStmt.setString(1, status);
            insertTokenPrepStmt.setString(2, domain);
            insertTokenPrepStmt.setString(3, tenantDomain);
            insertTokenPrepStmt.executeUpdate();
            connection.commit();
            return true;
        } catch (SQLException e) {
            throw new WSUserStoreException("Error occurred while deleting access for domain : " + domain,
                    e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, insertTokenPrepStmt);
        }
    }

    public boolean updateAccessToken(String oldToken, String newToken, String domain) throws WSUserStoreException {

        Connection connection = DatabaseUtil.getInstance().getDBConnection();
        PreparedStatement insertTokenPrepStmt = null;
        try {
            insertTokenPrepStmt = connection.prepareStatement(SQLQueries.ACCESS_TOKEN_UPDATE);
            insertTokenPrepStmt.setString(1, newToken);
            insertTokenPrepStmt.setString(2, oldToken);
            insertTokenPrepStmt.setString(3, domain);
            insertTokenPrepStmt.executeUpdate();
            connection.commit();
            return true;
        } catch (SQLException e) {
            throw new WSUserStoreException("Error occurred while updating access for domain : " + domain,
                    e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, insertTokenPrepStmt);
        }
    }
}
