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
import org.wso2.carbon.identity.user.store.outbound.dao.TokenMgtDao;
import org.wso2.carbon.identity.user.store.outbound.exception.WSUserStoreException;
import org.wso2.carbon.identity.user.store.outbound.messaging.JMSConnectionException;
import org.wso2.carbon.identity.user.store.outbound.messaging.JMSConnectionFactory;
import org.wso2.carbon.identity.user.store.outbound.model.AccessToken;
import org.wso2.carbon.identity.user.store.outbound.model.AgentConnection;
import org.wso2.carbon.identity.user.store.outbound.model.ServerOperation;
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

public class TokenMgtService extends AbstractAdmin {

    private static Log LOGGER = LogFactory.getLog(TokenMgtService.class);

    public boolean insertAccessToken(String domain, String token) {

        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        TokenMgtDao tokenMgtDao = new TokenMgtDao();
        AccessToken accessToken = new AccessToken();
        accessToken.setAccessToken(token);
        accessToken.setTenant(tenantDomain);
        accessToken.setDomain(domain);
        accessToken.setStatus("A"); //TODO constant
        try {
            return tokenMgtDao.insertAccessToken(accessToken);
        } catch (WSUserStoreException e) {
            LOGGER.error("Error occurred while inserting token", e);
        }
        return false;
    }

    public boolean deleteAccessToken(String domain) {

        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        TokenMgtDao tokenMgtDao = new TokenMgtDao();
        try {
            return tokenMgtDao.deleteAccessToken(tenantDomain, domain);
        } catch (WSUserStoreException e) {
            LOGGER.error("Error occurred while inserting token", e);
        }
        return false;
    }

    public boolean updateAccessToken(String oldToken, String newToken, String domain) {

        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        killAgentConnections(tenantDomain, domain);
        TokenMgtDao tokenMgtDao = new TokenMgtDao();
        try {
            tokenMgtDao.updateConnectionStatus(tenantDomain, domain, "F");
            return tokenMgtDao.updateAccessToken(oldToken, newToken, domain);
        } catch (WSUserStoreException e) {
            LOGGER.error("Error occurred while updating token", e);
        }
        return true;
    }

    public boolean validateAccessToken(String token) {

        return true;
    }

    public List<AgentConnection> getAgentConnections(String domain) {

        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        TokenMgtDao tokenMgtDao = new TokenMgtDao();
        try {
            return tokenMgtDao.getAgentConnections(tenantDomain, domain);
        } catch (WSUserStoreException e) {
            LOGGER.error("Error occurred while inserting token", e);
        }
        return Collections.emptyList();
    }

    public boolean deleteConnections(String domain) {

        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        killAgentConnections(tenantDomain, domain);
        TokenMgtDao tokenMgtDao = new TokenMgtDao();
        try {
            return tokenMgtDao.deleteConnections(tenantDomain, domain);
        } catch (WSUserStoreException e) {
            LOGGER.error("Error occurred while inserting token", e);
        }
        return false;
    }

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

    private void addNextServerOperation(String operationType, String domain, String tenantDomain,
            Session requestSession, MessageProducer producer, Destination responseQueue)
            throws JMSException, WSUserStoreException {

        TokenMgtDao tokenMgtDao = new TokenMgtDao();
        List<String> serverNodes = tokenMgtDao.getServerNodes(tenantDomain);
        for (String serverNode : serverNodes) {
            ServerOperation requestOperation = new ServerOperation();
            requestOperation.setTenantDomain(tenantDomain);
            requestOperation.setDomain(domain);
            requestOperation.setOperationType(operationType);
            ObjectMessage requestMessage = requestSession.createObjectMessage();
            requestMessage.setObject(requestOperation);
            requestMessage.setJMSExpiration(UserStoreConstants.QUEUE_MESSAGE_LIFETIME);

            requestMessage.setStringProperty("serverNode", serverNode);
            requestMessage.setJMSReplyTo(responseQueue);
            producer.send(requestMessage);
        }
    }
}
