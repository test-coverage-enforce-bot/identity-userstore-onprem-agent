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
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.identity.user.store.outbound.util.FileUtil;
import org.wso2.carbon.utils.CarbonUtils;

//TODO add a class comment
public class CloudDirectoryAdminService extends AbstractAdmin {

    private static Log log = LogFactory.getLog(CloudDirectoryAdminService.class);

    /**
     * Generate agent zip file
     *
     * @return Path to generated zip file
     */
    //TODO remove copying file to tmp directory and get from static and remove
    public String generateAgentFile() {

        String directoryName;
        try {
            FileUtil downloadUtil = new FileUtil();
            directoryName = FileUtil.getDirectoryNameInTimestamp();

            downloadUtil.copyFiles(CarbonUtils.getCarbonHome() + FileUtil.AGENT_STATIC_FILES_PATH,
                    CarbonUtils.getCarbonHome() + FileUtil.AGENT_TEMP_PATH + directoryName + "/");
        } catch (Exception e) {
            log.error("Error occurred while creating agent zip file.", e);
            return "";
        }
        return CarbonUtils.getCarbonHome() + FileUtil.AGENT_TEMP_PATH + directoryName + "/" + FileUtil.AGENT_FILE_NAME;
    }
}
