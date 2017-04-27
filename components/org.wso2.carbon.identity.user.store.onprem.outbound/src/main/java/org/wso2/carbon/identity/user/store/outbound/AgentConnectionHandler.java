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
import org.wso2.carbon.identity.user.store.common.UserStoreConstants;
import org.wso2.carbon.identity.user.store.common.messaging.JMSConnectionException;
import org.wso2.carbon.identity.user.store.common.messaging.JMSConnectionFactory;
import org.wso2.carbon.identity.user.store.common.model.ServerOperation;
import org.wso2.carbon.identity.user.store.outbound.dao.AgentConnectionMgtDao;
import org.wso2.carbon.identity.user.store.outbound.exception.WSUserStoreException;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.api.UserStoreException;

import java.util.List;
import java.util.Map;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

public class AgentConnectionHandler {

    private static Log LOGGER = LogFactory.getLog(AgentConnectionHandler.class);

    /**
     * Send a server operation message to kill already connected agent connections
     * @param tenantDomain
     * @param domain
     */
    public void killAgentConnections(String tenantDomain, String domain) {

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
            messageBrokerURL = userStoreProperties.get(UserStoreConstants.USER_STORE_PROPERTY_NAME_MESSAGE_BROKER_ENDPOINT);

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
                        .createTopicDestination(requestSession, UserStoreConstants.QUEUE_NAME_REQUEST);
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
     * @throws javax.jms.JMSException
     * @throws org.wso2.carbon.identity.user.store.outbound.exception.WSUserStoreException
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
            requestMessage.setJMSExpiration(UserStoreConstants.QUEUE_SERVER_MESSAGE_LIFETIME);

            requestMessage.setStringProperty(UserStoreConstants.UM_MESSAGE_SELECTOR_SERVER_NODE, serverNode);
            requestMessage.setJMSReplyTo(responseQueue);
            producer.send(requestMessage);
        }
    }
}
