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
package org.wso2.carbon.identity.user.store.outbound.dao;

public class SQLQueries {

    public static final String ACCESS_TOKEN_INSERT = "INSERT INTO UM_ACCESS_TOKEN(UM_TOKEN,UM_STATUS,UM_TENANT) " +
            "VALUES(?,?,?)";
    public static final String ACCESS_TOKEN_DELETE = "DELETE FROM UM_ACCESS_TOKEN WHERE UM_TENANT= ? ";
    public static final String NEXT_SERVER_NODE_GET = "SELECT A.UM_SERVER_NODE FROM UM_AGENT_CONNECTIONS " +
            "A,UM_ACCESS_TOKEN T WHERE A.UM_ACCESS_TOKEN = T.UM_TOKEN AND A.UM_STATUS=? AND T.UM_TENANT=? ORDER BY " +
            "A.UM_LASTUPDATEDATE;";
}
