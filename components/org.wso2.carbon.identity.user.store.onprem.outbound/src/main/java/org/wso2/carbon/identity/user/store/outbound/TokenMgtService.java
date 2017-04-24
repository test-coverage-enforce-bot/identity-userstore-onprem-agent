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
package org.wso2.carbon.identity.user.store.outbound;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.identity.user.store.common.UserStoreConstants;
import org.wso2.carbon.identity.user.store.common.messaging.JMSConnectionException;
import org.wso2.carbon.identity.user.store.common.messaging.JMSConnectionFactory;
import org.wso2.carbon.identity.user.store.outbound.dao.AgentConnectionMgtDao;
import org.wso2.carbon.identity.user.store.outbound.dao.TokenMgtDao;
import org.wso2.carbon.identity.user.store.outbound.exception.WSUserStoreException;
import org.wso2.carbon.identity.user.store.common.model.AccessToken;
import org.wso2.carbon.identity.user.store.common.model.AgentConnection;
import org.wso2.carbon.identity.user.store.common.model.ServerOperation;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.api.UserStoreException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

/**
 * Token management admin service
 */
public class TokenMgtService extends AbstractAdmin {

    private static Log LOGGER = LogFactory.getLog(TokenMgtService.class);

    /**
     * Insert access token
     * @param domain
     * @param token
     * @return
     */
    public boolean insertAccessToken(String domain, String token) {

        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        TokenMgtDao tokenMgtDao = new TokenMgtDao();
        AccessToken accessToken = new AccessToken();
        accessToken.setAccessToken(token);
        accessToken.setTenant(tenantDomain);
        accessToken.setDomain(domain);
        accessToken.setStatus(UserStoreConstants.ACCESS_TOKEN_STATUS_ACTIVE);
        try {
            return tokenMgtDao.insertAccessToken(accessToken);
        } catch (WSUserStoreException e) {
            LOGGER.error("Error occurred while inserting token for domain " + domain, e);
        }
        return false;
    }

    /**
     * Delete access token
     * @param domain
     * @return
     */
    public boolean deleteAccessToken(String domain) {

        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        TokenMgtDao tokenMgtDao = new TokenMgtDao();
        try {
            return tokenMgtDao.deleteAccessToken(tenantDomain, domain);
        } catch (WSUserStoreException e) {
            LOGGER.error("Error occurred while deleting token for domain " + domain, e);
        }
        return false;
    }

    /**
     * Update access token
     * @param oldToken
     * @param newToken
     * @param domain
     * @return
     */
    public boolean updateAccessToken(String oldToken, String newToken, String domain) {

        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        killAgentConnections(tenantDomain, domain);
        TokenMgtDao tokenMgtDao = new TokenMgtDao();
        AgentConnectionMgtDao agentConnectionMgtDao = new AgentConnectionMgtDao();
        try {
            agentConnectionMgtDao.updateConnectionStatus(tenantDomain, domain,
                    UserStoreConstants.CLIENT_CONNECTION_STATUS_CONNECTION_FAILED);
            return tokenMgtDao.updateAccessToken(oldToken, newToken, domain);
        } catch (WSUserStoreException e) {
            LOGGER.error("Error occurred while updating token for domain " + domain, e);
        }
        return true;
    }

    /**
     * Get all agent connections for user store domain
     * @param domain
     * @return
     */
    public List<AgentConnection> getAgentConnections(String domain) {

        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        AgentConnectionMgtDao agentConnectionMgtDao = new AgentConnectionMgtDao();
        try {
            return agentConnectionMgtDao.getAgentConnections(tenantDomain, domain);
        } catch (WSUserStoreException e) {
            LOGGER.error("Error occurred while getting agent connections for domain " + domain, e);
        }
        return Collections.emptyList();
    }

    /**
     * Delete agent connections
     * @param domain
     * @return
     */
    public boolean deleteConnections(String domain) {

        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        killAgentConnections(tenantDomain, domain);
        AgentConnectionMgtDao agentConnectionMgtDao = new AgentConnectionMgtDao();
        try {
            return agentConnectionMgtDao.deleteConnections(tenantDomain, domain);
        } catch (WSUserStoreException e) {
            LOGGER.error("Error occurred while deleting agent connections for domain " + domain, e);
        }
        return false;
    }

    /**
     * Send a server operation message to kill already connected agent connections
     * @param tenantDomain
     * @param domain
     */
    private void killAgentConnections(String tenantDomain, String domain) {

        String messageBrokerURL = null;
        RealmConfiguration secondaryRealmConfiguration = null;
        try {
            secondaryRealmConfiguration = CarbonContext.getThreadLocalCarbonContext().getUserRealm()
                    .getRealmConfiguration().getSecondaryRealmConfig();
        } catch (UserStoreException e) {
            LOGGER.error("Error occurred while reading user store information", e);
        }

        if (secondaryRealmConfiguration != null) {
            Map<String, String> userStoreProperties = secondaryRealmConfiguration.getUserStoreProperties();
            messageBrokerURL = userStoreProperties.get(UserStoreConstants.MESSAGE_BROKER_ENDPOINT);

            JMSConnectionFactory connectionFactory = new JMSConnectionFactory();
            Connection connection = null;
            Session requestSession;
            Destination requestQueue;
            Destination responseQueue;
            MessageProducer producer;
            try {
                connectionFactory.createActiveMQConnectionFactory(messageBrokerURL);
                connection = connectionFactory.createConnection();
                connectionFactory.start(connection);
                requestSession = connectionFactory.createSession(connection);
                requestQueue = connectionFactory
                        .createQueueDestination(requestSession, UserStoreConstants.QUEUE_NAME_REQUEST);
                producer = connectionFactory
                        .createMessageProducer(requestSession, requestQueue, DeliveryMode.NON_PERSISTENT);
                responseQueue = connectionFactory
                        .createQueueDestination(requestSession, UserStoreConstants.QUEUE_NAME_RESPONSE);
                addNextServerOperation(UserStoreConstants.SERVER_OPERATION_TYPE_KILL_AGENTS, domain, tenantDomain,
                        requestSession, producer, responseQueue);

            } catch (JMSConnectionException e) {
                LOGGER.error("Error occurred while adding message to queue", e);
            } catch (JMSException e) {
                LOGGER.error("Error occurred while adding message to queue", e);
            } catch (WSUserStoreException e) {
                LOGGER.error("Error occurred while adding message to queue", e);
            } finally {
                try {
                    connectionFactory.closeConnection(connection);
                } catch (JMSConnectionException e) {
                    LOGGER.error("Error occurred while closing the connection", e);
                }
            }
        }
    }

    /**
     * Send server operation message to queue
     * @param operationType
     * @param domain
     * @param tenantDomain
     * @param requestSession
     * @param producer
     * @param responseQueue
     * @throws JMSException
     * @throws WSUserStoreException
     */
    private void addNextServerOperation(String operationType, String domain, String tenantDomain,
            Session requestSession, MessageProducer producer, Destination responseQueue)
            throws JMSException, WSUserStoreException {

        AgentConnectionMgtDao AgentConnectionMgtDao = new AgentConnectionMgtDao();
        List<String> serverNodes = AgentConnectionMgtDao.getServerNodes(tenantDomain);
        for (String serverNode : serverNodes) {
            ServerOperation requestOperation = new ServerOperation();
            requestOperation.setTenantDomain(tenantDomain);
            requestOperation.setDomain(domain);
            requestOperation.setOperationType(operationType);
            ObjectMessage requestMessage = requestSession.createObjectMessage();
            requestMessage.setObject(requestOperation);
            requestMessage.setJMSExpiration(UserStoreConstants.QUEUE_MESSAGE_LIFETIME);

            requestMessage.setStringProperty(UserStoreConstants.UM_MESSAGE_SELECTOR_SERVER_NODE, serverNode);
            requestMessage.setJMSReplyTo(responseQueue);
            producer.send(requestMessage);
        }
    }
}
