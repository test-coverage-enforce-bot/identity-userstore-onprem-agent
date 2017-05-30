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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.identity.user.store.common.UserStoreConstants;
import org.wso2.carbon.identity.user.store.common.model.AccessToken;
import org.wso2.carbon.identity.user.store.outbound.dao.AgentConnectionMgtDao;
import org.wso2.carbon.identity.user.store.outbound.dao.TokenMgtDao;
import org.wso2.carbon.identity.user.store.outbound.exception.WSUserStoreException;

/**
 * Token management admin service
 */
public class TokenMgtService extends AbstractAdmin {

    private static Log LOGGER = LogFactory.getLog(TokenMgtService.class);

    /**
     * Get access token
     * @param domain User store domain name
     * @return access token
     */
    public String getActiveAccessToken(String domain) {

        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        TokenMgtDao tokenMgtDao = new TokenMgtDao();

        try {
            return tokenMgtDao.getAccessToken(tenantDomain, domain, UserStoreConstants.ACCESS_TOKEN_STATUS_ACTIVE);
        } catch (WSUserStoreException e) {
            LOGGER.error("Error occurred while getting token for domain " + domain, e);
        }
        return null;
    }

    /**
     * Insert access token
     * @param domain User store domain name
     * @param token Access token
     * @return result
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
            if (StringUtils.isEmpty(
                    tokenMgtDao.getAccessToken(tenantDomain, domain, UserStoreConstants.ACCESS_TOKEN_STATUS_ACTIVE))) {
                return tokenMgtDao.insertAccessToken(accessToken);
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Access token already exist for tenant: " + tenantDomain + " domain: " + domain);
                }
            }
        } catch (WSUserStoreException e) {
            LOGGER.error("Error occurred while inserting token for domain " + domain, e);
        }
        return false;
    }

    /**
     * Deactivate access token
     * @param domain User store domain name
     * @return result
     */
    public boolean deActivateAccessToken(String domain) {

        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        TokenMgtDao tokenMgtDao = new TokenMgtDao();
        try {
            return tokenMgtDao.updateAccessTokenStatus(tenantDomain, domain,
                    UserStoreConstants.ACCESS_TOKEN_STATUS_INACTIVE);
        } catch (WSUserStoreException e) {
            LOGGER.error("Error occurred while deleting token for domain " + domain, e);
        }
        return false;
    }

    /**
     * Revoke Old access token and regenerate new access token
     * @param oldToken Old access token
     * @param newToken New access token
     * @param domain User store domain name
     * @return result
     */
    public boolean revokeAndRegenerateAccessToken(String oldToken, String newToken, String domain) {

        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        AgentConnectionHandler connectionHandler = new AgentConnectionHandler();
        connectionHandler.killAgentConnections(tenantDomain, domain);
        TokenMgtDao tokenMgtDao = new TokenMgtDao();
        AgentConnectionMgtDao agentConnectionMgtDao = new AgentConnectionMgtDao();
        try {
            agentConnectionMgtDao.updateConnectionStatus(tenantDomain, domain,
                    UserStoreConstants.CLIENT_CONNECTION_STATUS_CONNECTION_FAILED);
            tokenMgtDao.updateAccessTokenStatus(oldToken, UserStoreConstants.ACCESS_TOKEN_STATUS_INACTIVE);

            AccessToken accessToken = new AccessToken();
            accessToken.setAccessToken(newToken);
            accessToken.setTenant(tenantDomain);
            accessToken.setDomain(domain);
            accessToken.setStatus(UserStoreConstants.ACCESS_TOKEN_STATUS_ACTIVE);
            return tokenMgtDao.insertAccessToken(accessToken);
        } catch (WSUserStoreException e) {
            LOGGER.error("Error occurred while updating token for domain " + domain, e);
        }
        return true;
    }

}
