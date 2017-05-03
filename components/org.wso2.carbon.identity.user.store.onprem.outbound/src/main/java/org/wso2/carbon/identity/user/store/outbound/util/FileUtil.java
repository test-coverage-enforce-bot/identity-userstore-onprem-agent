/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.user.store.outbound.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utility class to file operations such as create, delete, zip etc..
 */
public class FileUtil {

    private static Log log = LogFactory.getLog(FileUtil.class);
    public final static String AGENT_TEMP_PATH = "/repository/resources/agent/tmp/";
    public final static String AGENT_STATIC_FILES_PATH = "/repository/resources/agent/static";
    public final static String AGENT_FILE_NAME = "wso2agent.zip";


    /**
     * Get directory name with timestamp
     *
     * @return directory name
     */
    public static String getDirectoryNameInTimestamp() {
        long milliseconds = new Date().getTime();
        return Long.toString(milliseconds);
    }

    /**
     * Copy files
     *
     * @param sourcePath
     * @param destinationPath
     * @throws java.io.IOException
     */
    public void copyFiles(String sourcePath, String destinationPath) throws IOException {
        File source = new File(sourcePath);
        File destination = new File(destinationPath);
        FileUtils.copyDirectory(source, destination);
    }
}
