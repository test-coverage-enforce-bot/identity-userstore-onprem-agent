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

    public static final String ACCESS_TOKEN_INSERT = "INSERT INTO UM_ACCESS_TOKEN(UM_TOKEN,UM_STATUS,UM_TENANT," +
            "UM_DOMAIN)VALUES(?,?,?,?)";
    public static final String ACCESS_TOKEN_DELETE = "DELETE FROM UM_ACCESS_TOKEN WHERE UM_DOMAIN= ? AND UM_TENANT=?";
    public static final String ACCESS_TOKEN_UPDATE = "UPDATE UM_ACCESS_TOKEN SET UM_TOKEN=? WHERE UM_TOKEN=?" +
            " AND UM_DOMAIN=?";
    public static final String NEXT_SERVER_NODE_GET = "SELECT A.UM_SERVER_NODE FROM UM_AGENT_CONNECTIONS " +
            "A,UM_ACCESS_TOKEN T WHERE A.UM_ACCESS_TOKEN_ID = T.UM_ID AND A.UM_STATUS=? AND T.UM_TENANT=? ORDER BY " +
            "A.UM_LASTUPDATEDATE;";
    public static final String AGENT_CONNECTIONS_GET = "SELECT A.UM_TOKEN, C.UM_NODE, C.UM_STATUS FROM " +
            "UM_ACCESS_TOKEN A LEFT JOIN UM_AGENT_CONNECTIONS C ON A.UM_DOMAIN=? AND A.UM_TENANT=? AND A.UM_ID = " +
            "C.UM_ACCESS_TOKEN_ID ORDER BY C.UM_NODE;";
    public static final String AGENT_CONNECTIONS_DELETE_BY_DOMAIN = "DELETE FROM UM_AGENT_CONNECTIONS WHERE " +
            "UM_ACCESS_TOKEN_ID IN (SELECT A.UM_ID FROM UM_ACCESS_TOKEN A WHERE A.UM_DOMAIN=? AND A.UM_TENANT=?);";

    public static final String AGENT_CONNECTIONS_UPDATE_STATUS_BY_DOMAIN = "UPDATE UM_AGENT_CONNECTIONS " +
            "SET UM_STATUS=? WHERE UM_ACCESS_TOKEN_ID IN (SELECT A.UM_ID FROM UM_ACCESS_TOKEN A WHERE A.UM_DOMAIN=? " +
            "AND A.UM_TENANT=?);";


}
