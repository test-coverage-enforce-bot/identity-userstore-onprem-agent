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
package org.wso2.carbon.identity.user.store.common;

import org.wso2.carbon.identity.user.store.common.model.UserOperation;

/**
 * Message request utility to create user operation message which send to queue
 */
public class MessageRequestUtil {

    public static String getAuthenticationRequest(String userName, Object credential) {
        return String.format("{username : '%s', password : '%s'}", userName, credential);
    }

    public static String getUserPropertyValuesRequestData(String username, String attributes) {
        return String.format("{username : '%s', attributes : '%s'}", username, attributes);
    }

    public static String doGetExternalRoleListOfUserRequestData(String username) {
        return String.format("{username : '%s'}", username);
    }

    public static String getUserOperationJSONMessage(UserOperation userOperation) {
        return String
                .format("{correlationId : '%s', requestType : '%s', requestData : %s}",
                        userOperation.getCorrelationId(),
                        userOperation.getRequestType(), userOperation.getRequestData());
    }
}
