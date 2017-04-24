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
import org.wso2.carbon.identity.user.store.common.UserStoreConstants;
import org.wso2.carbon.identity.user.store.common.model.AgentConnection;
import org.wso2.carbon.identity.user.store.outbound.exception.WSUserStoreException;
import org.wso2.carbon.identity.user.store.outbound.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AgentConnectionMgtDao {

    /**
     * Get available server node information for a tenant
     * @param tenant
     * @return List of server nodes
     * @throws org.wso2.carbon.identity.user.store.outbound.exception.WSUserStoreException
     */
    public List<String> getServerNodes(String tenant) throws WSUserStoreException {
        Connection connection = DatabaseUtil.getInstance().getDBConnection();
        PreparedStatement insertTokenPrepStmt = null;
        ResultSet resultSet = null;
        List<String> serverNodes = new ArrayList<>();
        try {
            insertTokenPrepStmt = connection.prepareStatement(SQLQueries.NEXT_SERVER_NODE_GET);
            insertTokenPrepStmt.setString(1, UserStoreConstants.CLIENT_CONNECTION_STATUS_CONNECTED);
            insertTokenPrepStmt.setString(2, tenant);
            resultSet = insertTokenPrepStmt.executeQuery();
            while (resultSet.next()) {
                serverNodes.add(resultSet.getString("UM_SERVER_NODE"));
            }
        } catch (SQLException e) {
            throw new WSUserStoreException("Error occurred while reading server node info for tenant " + tenant, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet, insertTokenPrepStmt);
        }
        return serverNodes;
    }

    /**
     * Get agent connections for particular tenant and user store
     * @param tenantDomain
     * @param domain
     * @return List of connections
     * @throws org.wso2.carbon.identity.user.store.outbound.exception.WSUserStoreException
     */
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
            throw new WSUserStoreException("Error occurred while reading agent connection for tenant " + tenantDomain,
                    e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, resultSet, insertTokenPrepStmt);
        }
        return agentConnections;
    }

    /**
     * Delete agent connections
     * @param tenantDomain
     * @param domain
     * @return result of the delete operation
     * @throws org.wso2.carbon.identity.user.store.outbound.exception.WSUserStoreException
     */
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
            throw new WSUserStoreException("Error occurred while deleting agent connection for tenant : " + domain, e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, insertTokenPrepStmt);
        }
    }

    /**
     * Update agent connection status
     * @param tenantDomain
     * @param domain
     * @param status
     * @return Result of the update operation
     * @throws org.wso2.carbon.identity.user.store.outbound.exception.WSUserStoreException
     */
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
            throw new WSUserStoreException("Error occurred while updating connection status for tenant " + tenantDomain,
                    e);
        } finally {
            IdentityDatabaseUtil.closeAllConnections(connection, null, insertTokenPrepStmt);
        }
    }

}
