/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.carbon.identity.user.store.claim.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.user.store.claim.listener.cache.TenantDomainClaimCache;
import org.wso2.carbon.identity.user.store.claim.listener.cache.TenantDomainClaimCacheEntry;
import org.wso2.carbon.identity.user.store.claim.listener.cache.TenantDomainClaimCacheKey;
import org.wso2.carbon.identity.user.store.claim.listener.internal.WSUserStoreClaimListenerComponentHolder;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.user.store.common.UserStoreConstants;
import org.wso2.carbon.identity.user.store.common.messaging.JMSConnectionException;
import org.wso2.carbon.identity.user.store.common.messaging.JMSConnectionFactory;
import org.wso2.carbon.identity.user.store.common.model.UserOperation;
import org.wso2.carbon.user.api.ClaimManager;
import org.wso2.carbon.user.api.ClaimMapping;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.common.AbstractClaimManagerListener;

import java.util.Map;
import java.util.UUID;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

/**
 * Custom claim manager listener for secondary userstores.
 */
public class WSUserStoreClaimListener extends AbstractClaimManagerListener {
    private static Log log = LogFactory.getLog(WSUserStoreClaimListener.class);
    private int tenantId;
    private static String JMS_CORRELATIONID_FILTER = "JMSCorrelationID='%s'";
    private ClaimManager claimManager;
    private UserStoreManager secondaryUserStoreManager;
    private String tenantDomain;

    @Override
    public boolean getAttributeName(String domainName, String claimURI)
            throws org.wso2.carbon.user.core.UserStoreException {
        try {
            tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
            Tenant tenant = WSUserStoreClaimListenerComponentHolder.getInstance().getRealmService()
                    .getTenantManager().getTenant(tenantId);
            if (tenant != null) {
                tenantDomain = WSUserStoreClaimListenerComponentHolder.getInstance().getRealmService()
                        .getTenantManager().getTenant(tenantId).getDomain();
                TenantDomainClaimCacheEntry cacheEntry = getTenantDomainReferenceFromCache(
                        getTenantDomainCacheKey(domainName, tenantDomain));
                if (cacheEntry == null) {
                    secondaryUserStoreManager = ((UserStoreManager) (WSUserStoreClaimListenerComponentHolder.getInstance()
                            .getRealmService().getTenantUserRealm(tenantId).getUserStoreManager()))
                            .getSecondaryUserStoreManager(domainName);

                    if (secondaryUserStoreManager != null) {
                        claimManager = secondaryUserStoreManager.getClaimManager();
                        ClaimMapping[] claimMappings = claimManager.getAllClaimMappings();

                        JMSConnectionFactory connectionFactory = new JMSConnectionFactory();
                        RealmConfiguration secondaryRealmConfiguration = null;
                        Connection connection = null;
                        Session requestSession;
                        Session responseSession;
                        Destination requestQueue;
                        Destination responseQueue;
                        MessageProducer producer;
                        String messageBrokerURL;
                        int messageRetryLimit;
                        int messageConsumeLimit;
                        int messageLifetime;

                        try {
                            if (log.isDebugEnabled()) {
                                log.debug("Sending get all claims attribute request to queue for tenant: "
                                        + tenantDomain);
                            }
                            secondaryRealmConfiguration = CarbonContext.getThreadLocalCarbonContext().getUserRealm()
                                    .getRealmConfiguration().getSecondaryRealmConfig();
                            Map<String, String> userStoreProperties = secondaryRealmConfiguration
                                    .getUserStoreProperties();
                            messageBrokerURL = userStoreProperties
                                    .get(UserStoreConstants.USER_STORE_PROPERTY_NAME_MESSAGE_BROKER_ENDPOINT);
                            messageRetryLimit = Integer.parseInt(userStoreProperties
                                    .get(UserStoreConstants.USER_STORE_PROPERTY_NAME_MESSAGE_RETRY_LIMIT));
                            messageConsumeLimit = Integer.parseInt(userStoreProperties
                                    .get(UserStoreConstants.USER_STORE_PROPERTY_NAME_MESSAGE_CONSUME_TIMEOUT));
                            messageLifetime = Integer.parseInt(userStoreProperties
                                    .get(UserStoreConstants.USER_STORE_PROPERTY_NAME_MESSAGE_LIFETIME));

                            connectionFactory.createActiveMQConnectionFactory(messageBrokerURL);
                            connection = connectionFactory.createConnection();
                            connectionFactory.start(connection);
                            requestSession = connectionFactory.createSession(connection);
                            requestQueue = connectionFactory
                                    .createTopicDestination(requestSession, UserStoreConstants.TOPIC_NAME_REQUEST);
                            producer = connectionFactory
                                    .createMessageProducer(requestSession, requestQueue, DeliveryMode.NON_PERSISTENT);

                            Message responseMessage = null;
                            int retryCount = 0;
                            while (responseMessage == null && messageRetryLimit > retryCount) {

                                if (log.isDebugEnabled()) {
                                    log.debug("Trying to get all claims count: " + retryCount);
                                }
                                String correlationId = UUID.randomUUID().toString();
                                responseQueue = connectionFactory
                                        .createQueueDestination(requestSession, UserStoreConstants.QUEUE_NAME_RESPONSE);
                                addNextUserOperationToTopic(correlationId,
                                        UserStoreConstants.UM_OPERATION_TYPE_GET_ALL_ATTRIBUTES, "", requestSession,
                                        producer, responseQueue, domainName, messageLifetime);

                                responseSession = connectionFactory.createSession(connection);
                                String filter = String.format(JMS_CORRELATIONID_FILTER, correlationId);
                                MessageConsumer consumer = responseSession.createConsumer(responseQueue, filter);
                                responseMessage = consumer.receive(messageConsumeLimit);
                                retryCount++;
                            }

                            if (responseMessage != null) {
                                UserOperation response = (UserOperation) ((ObjectMessage) responseMessage).getObject();
                                JSONObject jsonResponse = new JSONObject(response.getResponseData());
                                JSONObject jsonObject = new JSONObject(jsonResponse
                                        .get(UserStoreConstants.UM_JSON_ELEMENT_RESPONSE_DATA_RESULT).toString());

                                String cacheKey = getTenantDomainCacheKey(domainName, tenantDomain);
                                addTenantDomainClaimToCache(cacheKey, claimURI);
                                for (ClaimMapping claimMapping : claimMappings) {
                                    String uri = claimMapping.getClaim().getClaimUri();
                                    try {
                                        String attribute = (String) jsonObject.getJSONObject("attributes").get(uri);
                                        if (attribute != null && !attribute.isEmpty()) {
                                            updateClaimMapping(domainName, claimMapping, attribute);
                                        }
                                    } catch (JSONException e) {
                                        if (log.isDebugEnabled()) {
                                            log.debug("Claim mapping of " + uri
                                                    + " not found for secondary user store of tenant " + tenantId);
                                        }
                                    }
                                }
                            } else {
                                if (log.isDebugEnabled()) {
                                    log.debug("Get all claim attributes failed due to response object is null");
                                }
                            }
                        } catch (JMSConnectionException e) {
                            log.error("Error occurred while creating JMS connection", e);
                        } catch (JMSException e) {
                            log.error("Error occurred while adding message to queue", e);
                        } finally {
                            try {
                                connectionFactory.closeConnection(connection);
                            } catch (JMSConnectionException e) {
                                log.error("Error occurred while closing the connection", e);
                            }
                        }

                    }
                }
            }
        } catch (UserStoreException e) {
            throw new org.wso2.carbon.user.core.UserStoreException(
                    "Error occurred while calling backed to claim attribute " +
                            "retrieval for tenantId - [" + this.tenantId + "]", e);
        }

        return true;
    }

    /**
     * Add next user operation to queue
     * @param correlationId Connection Id
     * @param operationType Operation type ex. authenticate, getuserlist etc.
     * @param requestData Request data ex. username/password
     * @param requestSession JMS session
     * @param producer JMS Producer
     * @param responseQueue Destination queue to add the message
     * @throws JMSException
     */
    private void addNextUserOperationToTopic(String correlationId, String operationType, String requestData,
            Session requestSession, MessageProducer producer, Destination responseQueue, String userStoreDomain,
            long messageLifetime)
            throws JMSException {

        String tenantDomain = IdentityTenantUtil.getTenantDomain(tenantId);

        UserOperation requestOperation = new UserOperation();
        requestOperation.setCorrelationId(correlationId);
        requestOperation.setRequestData(requestData);
        requestOperation.setTenant(tenantDomain);
        requestOperation.setRequestType(operationType);
        requestOperation.setDomain(userStoreDomain);

        ObjectMessage requestMessage = requestSession.createObjectMessage();
        requestMessage.setObject(requestOperation);
        requestMessage.setJMSCorrelationID(correlationId);
        requestMessage.setJMSExpiration(messageLifetime);
        requestMessage.setJMSReplyTo(responseQueue);
        producer.send(requestMessage);
    }

    private void updateClaimMapping( String domainName, ClaimMapping claimMapping, String attribute)
            throws UserStoreException {

        if (attribute != claimMapping.getMappedAttribute(domainName)) {
            claimMapping.getMappedAttributes().put(domainName, attribute);
            claimManager.updateClaimMapping(claimMapping);
        }
    }


    private TenantDomainClaimCacheEntry getTenantDomainReferenceFromCache(String domainName) {

        TenantDomainClaimCacheKey cacheKey = new TenantDomainClaimCacheKey(domainName);
        return TenantDomainClaimCache.getInstance().getValueFromCache(cacheKey);
    }


    private void addTenantDomainClaimToCache(String tenantDomain, String reference) {

        TenantDomainClaimCacheKey cacheKey = new TenantDomainClaimCacheKey(tenantDomain);
        TenantDomainClaimCacheEntry cacheEntry = new TenantDomainClaimCacheEntry();
        cacheEntry.setDomainReference(reference);
        TenantDomainClaimCache.getInstance().addToCache(cacheKey, cacheEntry);
    }



    private String getTenantDomainCacheKey( String domainName, String tenantDomain){
        return tenantDomain + "-" + domainName;
    }

}
