/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.identity.user.store.common;

/**
 * User store constants
 */
public class UserStoreConstants {

    public final static String UM_OPERATION_TYPE_AUTHENTICATE = "authenticate";
    public final static String UM_OPERATION_TYPE_GET_CLAIMS = "getclaims";
    public final static String UM_OPERATION_TYPE_GET_USER_ROLES = "getuserroles";
    public final static String UM_OPERATION_TYPE_GET_ROLES = "getroles";
    public final static String UM_OPERATION_TYPE_GET_USER_LIST = "getuserlist";
    public final static String UM_OPERATION_TYPE_ERROR = "error";

    public final static String SERVER_OPERATION_TYPE_KILL_AGENTS = "killagents";

    public final static String UM_OPERATION_AUTHENTICATE_RESULT_SUCCESS = "SUCCESS";

    public static final String CLIENT_CONNECTION_STATUS_CONNECTED = "C";
    public static final String CLIENT_CONNECTION_STATUS_CONNECTION_FAILED = "F";

    public static final String ACCESS_TOKEN_STATUS_ACTIVE = "A";
    public final static String QUEUE_NAME_REQUEST = "requestQueue";
    public final static String QUEUE_NAME_RESPONSE = "responseQueue";
    public final static String USER_STORE_PROPERTY_NAME_MESSAGE_BROKER_ENDPOINT = "MessageBrokerEndPointURL";
    public final static String USER_STORE_PROPERTY_NAME_MESSAGE_CONSUME_TIMEOUT = "MessageConsumeTimeout";
    public final static String USER_STORE_PROPERTY_NAME_MESSAGE_LIFETIME = "MessageLifetime";
    public final static String USER_STORE_PROPERTY_NAME_MESSAGE_RETRY_LIMIT = "MessageRetryLimit";
    public final static String USER_STORE_PROPERTY_NAME_DIRECTORY_NAME = "DirectoryName";
    public final static long QUEUE_SERVER_MESSAGE_LIFETIME = 5 * 60 * 1000;
}
