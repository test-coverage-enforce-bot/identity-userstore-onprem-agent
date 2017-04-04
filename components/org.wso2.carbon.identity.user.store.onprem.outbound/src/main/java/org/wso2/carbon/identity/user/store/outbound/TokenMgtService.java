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
import org.wso2.carbon.identity.user.store.outbound.model.AccessToken;

public class TokenMgtService extends AbstractAdmin {

    private static Log LOGGER = LogFactory.getLog(TokenMgtService.class);

    public boolean insertAccessToken(String token) {

        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        TokenMgtDao tokenMgtDao = new TokenMgtDao();
        AccessToken accessToken = new AccessToken();
        accessToken.setAccessToken(token);
        accessToken.setTenant(tenantDomain);
        accessToken.setStatus("A");
        try {
            return tokenMgtDao.insertAccessToken(accessToken);
        } catch (WSUserStoreException e) {
            LOGGER.error("Error occurred while inserting token", e);
        }
        return false;
    }

    public boolean deleteAccessToken(String tenantDomain) {

        TokenMgtDao tokenMgtDao = new TokenMgtDao();
        try {
            return tokenMgtDao.deleteAccessToken(tenantDomain);
        } catch (WSUserStoreException e) {
            LOGGER.error("Error occurred while inserting token", e);
        }
        return false;
    }

    public boolean updateAccessToken(String token) {

        return true;
    }

    public boolean validateAccessToken(String token) {

        return true;
    }
}
