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
import org.wso2.carbon.identity.user.store.common.model.AgentConnection;
import org.wso2.carbon.identity.user.store.outbound.dao.AgentConnectionMgtDao;
import org.wso2.carbon.identity.user.store.outbound.exception.WSUserStoreException;

import java.util.Collections;
import java.util.List;

/**
 * Agent connection management admin service
 */
public class AgentMgtService extends AbstractAdmin {

    private static Log LOGGER = LogFactory.getLog(AgentMgtService.class);

    /**
     * Get all agent connections for user store domain
     * @param domain User store domain
     * @return List of agent connections
     */
    public List<AgentConnection> getAgentConnections(String domain) {

        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        AgentConnectionMgtDao agentConnectionMgtDao = new AgentConnectionMgtDao();
        try {
            return agentConnectionMgtDao.getAgentConnections(tenantDomain, domain);
        } catch (WSUserStoreException e) {
            LOGGER.error("Error occurred while getting agent connections for domain: " + domain, e);
        }
        return Collections.emptyList();
    }

    /**
     * Delete agent connections
     * @param domain User store domain
     * @return result
     */
    public boolean deleteConnections(String domain) {

        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        AgentConnectionHandler connectionHandler = new AgentConnectionHandler();
        connectionHandler.killAgentConnections(tenantDomain, domain);
        AgentConnectionMgtDao agentConnectionMgtDao = new AgentConnectionMgtDao();
        try {
            return agentConnectionMgtDao.deleteConnections(tenantDomain, domain);
        } catch (WSUserStoreException e) {
            LOGGER.error("Error occurred while deleting agent connections for domain " + domain, e);
        }
        return false;
    }

}
