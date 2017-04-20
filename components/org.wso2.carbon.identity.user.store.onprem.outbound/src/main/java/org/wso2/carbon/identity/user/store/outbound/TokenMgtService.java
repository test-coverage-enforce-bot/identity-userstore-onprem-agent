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
import org.wso2.carbon.identity.user.store.outbound.model.UserOperation;

import java.util.Collections;
import java.util.List;
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

public class TokenMgtService extends AbstractAdmin {

    private static Log LOGGER = LogFactory.getLog(TokenMgtService.class);

    public boolean insertAccessToken(String tenantDomain, String domain, String token) {

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

    public boolean deleteAccessToken(String tenantDomain, String domain) {

        TokenMgtDao tokenMgtDao = new TokenMgtDao();
        try {
            return tokenMgtDao.deleteAccessToken(tenantDomain, domain);
        } catch (WSUserStoreException e) {
            LOGGER.error("Error occurred while inserting token", e);
        }
        return false;
    }

    public boolean updateAccessToken(String oldToken, String newToken, String domain) {

        TokenMgtDao tokenMgtDao = new TokenMgtDao();
        try {
            return tokenMgtDao.updateAccessToken(oldToken, newToken, domain);
        } catch (WSUserStoreException e) {
            LOGGER.error("Error occurred while updating token", e);
        }
        return true;
    }

    public boolean validateAccessToken(String token) {

        return true;
    }

    public List<AgentConnection> getAgentConnections(String tenantDomain, String domain) {
        TokenMgtDao tokenMgtDao = new TokenMgtDao();
        try {
            return tokenMgtDao.getAgentConnections(tenantDomain, domain);
        } catch (WSUserStoreException e) {
            LOGGER.error("Error occurred while inserting token", e);
        }
        return Collections.emptyList();
    }

    public boolean deleteConnections(String tenantDomain, String domain) {

        TokenMgtDao tokenMgtDao = new TokenMgtDao();
        try {
            return tokenMgtDao.deleteConnections(tenantDomain, domain);
        } catch (WSUserStoreException e) {
            LOGGER.error("Error occurred while inserting token", e);
        }
        return false;
    }
}
