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
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.user.store.claim.listener.cache.TenantDomainClaimCache;
import org.wso2.carbon.identity.user.store.claim.listener.cache.TenantDomainClaimCacheEntry;
import org.wso2.carbon.identity.user.store.claim.listener.cache.TenantDomainClaimCacheKey;
import org.wso2.carbon.identity.user.store.claim.listener.internal.WSUserStoreClaimListenerComponentHolder;
import org.wso2.carbon.user.api.ClaimManager;
import org.wso2.carbon.user.api.ClaimMapping;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.common.AbstractClaimManagerListener;

/**
 * Custom claim manager listener for secondary userstores.
 */
public class WSUserStoreClaimListener extends AbstractClaimManagerListener {
    private static Log log = LogFactory.getLog(WSUserStoreClaimListener.class);
    private int tenantId;
    private ClaimManager claimManager;
    private UserStoreManager secondaryUserStoreManager;
    private String tenantDomain;

    @Override
    public boolean getAttributeName(String domainName, String claimURI)
            throws org.wso2.carbon.user.core.UserStoreException {
        try {
            if ("IS-WSO2.COM".equalsIgnoreCase(domainName)) {
                tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
                Tenant tenant = WSUserStoreClaimListenerComponentHolder.getInstance().getRealmService()
                        .getTenantManager().getTenant(tenantId);
                if (tenant != null) {
                    tenantDomain = WSUserStoreClaimListenerComponentHolder.getInstance().getRealmService()
                            .getTenantManager().getTenant(tenantId).getDomain();
                    TenantDomainClaimCacheEntry cacheEntry = getTenantDomainReferenceFromCache(
                            getTenantDomainCacheKey(domainName, tenantDomain));
                    if (cacheEntry == null) {
                        secondaryUserStoreManager = ((UserStoreManager) (WSUserStoreClaimListenerComponentHolder
                                .getInstance()
                                .getRealmService().getTenantUserRealm(tenantId).getUserStoreManager()))
                                .getSecondaryUserStoreManager(domainName);

                        if (secondaryUserStoreManager != null) {
                            claimManager = secondaryUserStoreManager.getClaimManager();
                            ClaimMapping[] claimMappings = claimManager.getAllClaimMappings();

                            String cacheKey = getTenantDomainCacheKey(domainName, tenantDomain);
                            addTenantDomainClaimToCache(cacheKey, claimURI);
                            for (ClaimMapping claimMapping : claimMappings) {
                                String uri = claimMapping.getClaim().getClaimUri();
                                updateClaimMapping(domainName, claimMapping, uri);
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

    private void updateClaimMapping( String domainName, ClaimMapping claimMapping, String attribute)
            throws UserStoreException {

        if (!attribute.equals(claimMapping.getMappedAttribute(domainName))) {
            claimMapping.getMappedAttributes().put(domainName, attribute);
            claimManager.updateClaimMapping(claimMapping);
            if (log.isDebugEnabled()) {
                log.debug("Replaced claim mapping for: " + claimMapping.getClaim().getClaimUri() + " with mapped " +
                        "attribute: " + attribute + " in userstore domain: " + domainName + " for tenant: " +
                        tenantDomain);
            }
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
