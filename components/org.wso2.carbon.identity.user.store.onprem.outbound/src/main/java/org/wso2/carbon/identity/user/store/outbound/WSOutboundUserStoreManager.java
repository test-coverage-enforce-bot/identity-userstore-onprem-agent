/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
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
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.user.store.outbound.messaging.JMSConnectionException;
import org.wso2.carbon.identity.user.store.outbound.messaging.JMSConnectionFactory;
import org.wso2.carbon.identity.user.store.outbound.model.UserOperation;
import org.wso2.carbon.user.api.Properties;
import org.wso2.carbon.user.api.Property;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.wso2.carbon.user.core.UserCoreConstants;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.claim.ClaimManager;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;
import org.wso2.carbon.user.core.common.RoleContext;
import org.wso2.carbon.user.core.profile.ProfileConfigurationManager;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.user.core.util.DatabaseUtil;
import org.wso2.carbon.user.core.util.JDBCRealmUtil;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WSOutboundUserStoreManager extends AbstractUserStoreManager {

    private static Log LOGGER = LogFactory.getLog(WSOutboundUserStoreManager.class);
    private final static String QUEUE_NAME_REQUEST = "requestQueue";
    private final static String QUEUE_NAME_RESPONSE = "responseQueue";

    public WSOutboundUserStoreManager() {

    }

    /**
     * @param realmConfig
     * @param tenantId
     * @throws UserStoreException
     */
    public WSOutboundUserStoreManager(RealmConfiguration realmConfig, int tenantId) throws UserStoreException {
        this.realmConfig = realmConfig;
        this.tenantId = tenantId;

        if (realmConfig.getUserStoreProperty(UserCoreConstants.RealmConfig.READ_GROUPS_ENABLED) != null) {
            readGroupsEnabled = Boolean.parseBoolean(realmConfig
                    .getUserStoreProperty(UserCoreConstants.RealmConfig.READ_GROUPS_ENABLED));
        }

        if (LOGGER.isDebugEnabled()) {
            if (readGroupsEnabled) {
                LOGGER.debug("ReadGroups is enabled for " + getMyDomainName());
            } else {
                LOGGER.debug("ReadGroups is disabled for " + getMyDomainName());
            }
        }

        if (realmConfig.getUserStoreProperty(UserCoreConstants.RealmConfig.WRITE_GROUPS_ENABLED) != null) {
            writeGroupsEnabled = Boolean.parseBoolean(realmConfig
                    .getUserStoreProperty(UserCoreConstants.RealmConfig.WRITE_GROUPS_ENABLED));
        } else {
            if (!isReadOnly()) {
                writeGroupsEnabled = true;
            }
        }

        if (LOGGER.isDebugEnabled()) {
            if (writeGroupsEnabled) {
                LOGGER.debug("WriteGroups is enabled for " + getMyDomainName());
            } else {
                LOGGER.debug("WriteGroups is disabled for " + getMyDomainName());
            }
        }

        if (writeGroupsEnabled) {
            readGroupsEnabled = true;
        }

	/* Initialize user roles cache as implemented in AbstractUserStoreManager */
        initUserRolesCache();
    }

    public WSOutboundUserStoreManager(org.wso2.carbon.user.api.RealmConfiguration realmConfig,
            Map<String, Object> properties,
            ClaimManager claimManager,
            ProfileConfigurationManager profileManager,
            UserRealm realm, Integer tenantId)
            throws UserStoreException {

        this(realmConfig, tenantId);
        this.realmConfig = realmConfig;
        this.tenantId = tenantId;
        this.userRealm = realm;

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Started " + System.currentTimeMillis());
        }
        this.claimManager = claimManager;
        this.userRealm = realm;

        dataSource = (org.apache.tomcat.jdbc.pool.DataSource) properties.get(UserCoreConstants.DATA_SOURCE);
        if (dataSource == null) {
            dataSource = DatabaseUtil.getRealmDataSource(realmConfig);
        }
        if (dataSource == null) {
            throw new UserStoreException("User Management Data Source is null");
        }

        properties.put(UserCoreConstants.DATA_SOURCE, dataSource);
        realmConfig.setUserStoreProperties(JDBCRealmUtil.getSQL(realmConfig.getUserStoreProperties()));

        this.persistDomain();
        doInitialSetup();
        if (realmConfig.isPrimary()) {
            addInitialAdminData(Boolean.parseBoolean(realmConfig.getAddAdmin()),
                    !isInitSetupDone());
        }

        initUserRolesCache();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Ended " + System.currentTimeMillis());
        }
    }

    @Override
    public boolean doAuthenticate(String userName, Object credential) throws UserStoreException {
        if (userName != null && credential != null) {
            return processAuthenticationRequest(userName, credential);
        }
        return false;
    }

    private String getAuthenticationRequest(String userName, Object credential) {
        return String.format("{username : '%s', password : '%s'}", userName, credential);
    }

    private boolean processAuthenticationRequest(String userName, Object credential) {

        JMSConnectionFactory connectionFactory = new JMSConnectionFactory();
        Connection connection = null;
        Session requestSession;
        Session responseSession;
        Destination requestQueue;
        Destination responseQueue;
        MessageProducer producer;
        try {
            connectionFactory.createActiveMQConnectionFactory();
            connection = connectionFactory.createConnection();
            connectionFactory.start(connection);
            requestSession = connectionFactory.createSession(connection);
            requestQueue = connectionFactory.createQueueDestination(requestSession, QUEUE_NAME_REQUEST);
            producer = connectionFactory
                    .createMessageProducer(requestSession, requestQueue, DeliveryMode.NON_PERSISTENT);

            String correlationId = UUID.randomUUID().toString();
            responseQueue = connectionFactory.createQueueDestination(requestSession, QUEUE_NAME_RESPONSE);

            addNextOperation(correlationId, OperationsConstants.UM_OPERATION_TYPE_AUTHENTICATE,
                    getAuthenticationRequest(userName, credential), requestSession, producer, responseQueue);

            responseSession = connectionFactory.createSession(connection);

            String filter = String.format("JMSCorrelationID='%s'", correlationId);
            MessageConsumer consumer = responseSession.createConsumer(responseQueue, filter);
            Message rm = consumer.receive(6000);
            UserOperation response = (UserOperation) ((ObjectMessage) rm).getObject();
            return OperationsConstants.UM_OPERATION_AUTHENTICATE_RESULT_SUCCESS.equals(response.getResponseData());
        } catch (JMSConnectionException e) {
            LOGGER.error("Error occurred while adding message to queue");
        } catch (JMSException e) {
            LOGGER.error("Error occurred while adding message to queue");
        } finally {
            try {
                connectionFactory.closeConnection(connection);
            } catch (JMSConnectionException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    private void addNextOperation(String correlationId, String operationType, String requestData,
            Session requestSession, MessageProducer producer, Destination responseQueue) throws JMSException {

        String tenantDomain = IdentityTenantUtil.getTenantDomain(tenantId);

        UserOperation requestOperation = new UserOperation();
        requestOperation.setCorrelationId(correlationId);
        requestOperation.setRequestData(requestData);
        requestOperation.setTenant(tenantDomain);
        requestOperation.setRequestType(operationType);

        ObjectMessage requestMessage = requestSession.createObjectMessage();
        requestMessage.setObject(requestOperation);
        requestMessage.setJMSCorrelationID(correlationId);

        requestMessage.setJMSReplyTo(responseQueue);
        producer.send(requestMessage);

    }

    @Override
    protected void doAddUser(String userName, Object credential, String[] roleList, Map<String, String> claims,
            String profileName, boolean requirePasswordChange) throws UserStoreException {
        throw new UserStoreException("UserStoreManager method not supported : #doAddUser");

    }

    @Override
    protected void doUpdateCredential(String userName, Object newCredential, Object oldCredential)
            throws UserStoreException {
        throw new UserStoreException("UserStoreManager method not supported : #doUpdateCredential");

    }

    @Override
    protected void doUpdateCredentialByAdmin(String userName, Object newCredential) throws UserStoreException {
        throw new UserStoreException("UserStoreManager method not supported : #doUpdateCredentialByAdmin");

    }

    @Override
    protected void doDeleteUser(String userName) throws UserStoreException {
        throw new UserStoreException("UserStoreManager method not supported : #doDeleteUser");

    }

    @Override
    protected void doSetUserClaimValue(String userName, String claimURI, String claimValue, String profileName)
            throws UserStoreException {
        throw new UserStoreException("UserStoreManager method not supported : #doSetUserClaimValue");

    }

    @Override
    protected void doSetUserClaimValues(String userName, Map<String, String> claims, String profileName)
            throws UserStoreException {
        throw new UserStoreException("UserStoreManager method not supported : #doSetUserClaimValues");

    }

    @Override
    protected void doDeleteUserClaimValue(String userName, String claimURI, String profileName)
            throws UserStoreException {
        throw new UserStoreException("UserStoreManager method not supported : #doDeleteUserClaimValue");

    }

    @Override
    protected void doDeleteUserClaimValues(String userName, String[] claims, String profileName)
            throws UserStoreException {
        throw new UserStoreException("UserStoreManager method not supported : #doDeleteUserClaimValues");

    }

    @Override
    protected void doUpdateUserListOfRole(String roleName, String[] deletedUsers, String[] newUsers)
            throws UserStoreException {
        throw new UserStoreException("UserStoreManager method not supported : #doUpdateUserListOfRole");

    }

    @Override
    protected void doUpdateRoleListOfUser(String userName, String[] deletedRoles, String[] newRoles)
            throws UserStoreException {
        throw new UserStoreException("UserStoreManager method not supported : #doUpdateRoleListOfUser");

    }

    public Map<String, String> getUserPropertyValues(String userName, String[] propertyNames, String profileName)
            throws UserStoreException {

        Map<String, String> mapAttributes = new HashMap<>();
        return mapAttributes; //TODO implement this
    }

    //Todo: Implement doCheckExistingRole
    @Override
    protected boolean doCheckExistingRole(String roleName) throws UserStoreException {
        return true;
    }

    @Override
    protected RoleContext createRoleContext(String roleName) throws UserStoreException {
        throw new UserStoreException("UserStoreManager method not supported : #createRoleContext");
    }

    //Todo: Implement doCheckExistingUser
    @Override
    protected boolean doCheckExistingUser(String userName) throws UserStoreException {
        return true;
    }

    @Override
    protected String[] getUserListFromProperties(String property, String value, String profileName)
            throws UserStoreException {
        throw new UserStoreException("UserStoreManager method not supported : #getUserListFromProperties");
    }

    @Override
    public String[] getProfileNames(String userName) throws UserStoreException {
        throw new UserStoreException("UserStoreManager method not supported : #getProfileNames");
    }

    @Override
    public String[] getAllProfileNames() throws UserStoreException {
        throw new UserStoreException("UserStoreManager method not supported : #getAllProfileNames");
    }

    @Override
    public boolean isReadOnly() throws UserStoreException {
        return "true".equalsIgnoreCase(realmConfig
                .getUserStoreProperty(UserCoreConstants.RealmConfig.PROPERTY_READ_ONLY));
    }

    public Date getPasswordExpirationTime(String userName) throws UserStoreException {
        throw new UserStoreException("UserStoreManager method not supported : #getPasswordExpirationTime");
    }

    @Override
    public int getUserId(String username) throws UserStoreException {
        throw new UserStoreException("UserStoreManager method not supported : #getUserId");
    }

    @Override
    public int getTenantId(String username) throws UserStoreException {
        throw new UserStoreException("UserStoreManager method not supported : #getTenantId");
    }

    @Override
    public int getTenantId() throws UserStoreException {
        return this.tenantId;
    }

    @Override
    public Map<String, String> getProperties(org.wso2.carbon.user.api.Tenant tenant) throws UserStoreException {
        throw new UserStoreException("UserStoreManager method not supported : #getProperties");
    }

    @Override
    public boolean isMultipleProfilesAllowed() {
        return false;
    }

    @Override
    public void addRememberMe(String s, String s1) throws org.wso2.carbon.user.api.UserStoreException {
        throw new UserStoreException("UserStoreManager method not supported : #addRememberMe");

    }

    @Override
    public boolean isValidRememberMeToken(String s, String s1) throws org.wso2.carbon.user.api.UserStoreException {
        throw new UserStoreException("UserStoreManager method not supported : #isValidRememberMeToken");
    }

    @Override
    public Map<String, String> getProperties(Tenant tenant) throws UserStoreException {
        throw new UserStoreException("UserStoreManager method not supported : #getProperties");
    }

    @Override
    public boolean isBulkImportSupported() {
        return new Boolean(this.realmConfig.getUserStoreProperty("IsBulkImportSupported"));
    }

    public Properties getDefaultUserStoreProperties() {

        Properties properties = new Properties();
        Property endpoint = new Property("Message Broker URL", "", "Message Broker connection URL", null);
        Property disabled = new Property("Disabled", "false", "Disabled#Check to disable the user store", null);

        Property[] mandatoryProperties = new Property[] { endpoint };
        Property[] optionalProperties = new Property[] { disabled };

        properties.setOptionalProperties(optionalProperties);
        properties.setMandatoryProperties(mandatoryProperties);
        return properties;
    }

    @Override
    public RealmConfiguration getRealmConfiguration() {
        return this.realmConfig;
    }

    @Override
    protected String[] doGetSharedRoleNames(String tenantDomain, String filter, int maxItemLimit)
            throws UserStoreException {
        throw new UserStoreException("UserStoreManager method not supported : #doGetSharedRoleNames");
    }

    //Todo: Implement doGetUserListOfRole
    @Override
    protected String[] doGetUserListOfRole(String roleName, String filter) throws UserStoreException {
        return null;
    }

    public String[] doListUsers(String filter, int maxItemLimit) throws UserStoreException {

        return new String[] { "" };//TODO implement this
    }

    @Override
    protected String[] doGetDisplayNamesForInternalRole(String[] userNames) throws UserStoreException {
        throw new UserStoreException("UserStoreManager method not supported : #doGetDisplayNamesForInternalRole");
    }

    @Override
    public boolean doCheckIsUserInRole(String userName, String roleName) throws UserStoreException {
        String[] roles = this.doGetExternalRoleListOfUser(userName, "*");
        if (roles != null) {
            String[] arr$ = roles;
            int len$ = roles.length;

            for (int i$ = 0; i$ < len$; ++i$) {
                String role = arr$[i$];
                if (role.equalsIgnoreCase(roleName)) {
                    return true;
                }
            }
        }

        return false;
    }

    public String[] doGetExternalRoleListOfUser(String userName, String filter) throws UserStoreException {

        return new String[] { "" }; //TODO implement this
    }

    @Override
    protected String[] doGetSharedRoleListOfUser(String userName, String tenantDomain, String filter)
            throws UserStoreException {
        throw new UserStoreException("UserStoreManager method not supported : #doGetSharedRoleListOfUser");
    }

    @Override
    protected void doAddRole(String roleName, String[] userList, boolean shared) throws UserStoreException {
        throw new UserStoreException("UserStoreManager method not supported : #doAddRole");

    }

    @Override
    protected void doDeleteRole(String roleName) throws UserStoreException {
        throw new UserStoreException("UserStoreManager method not supported : #doDeleteRole");

    }

    @Override
    protected void doUpdateRoleName(String roleName, String newRoleName) throws UserStoreException {
        throw new UserStoreException("UserStoreManager method not supported : #doUpdateRoleName");

    }

    public String[] doGetRoleNames(String filter, int maxItemLimit) throws UserStoreException {
        return new String[] { "" }; //TODO implement this
    }
}