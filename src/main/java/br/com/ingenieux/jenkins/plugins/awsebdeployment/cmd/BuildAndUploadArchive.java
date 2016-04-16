/*
 * Copyright 2011 ingenieux Labs
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package br.com.ingenieux.jenkins.plugins.awsebdeployment.cmd;

import br.com.ingenieux.jenkins.plugins.awsebdeployment.Utils;
import com.amazonaws.services.elasticbeanstalk.model.CreateStorageLocationResult;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import hudson.FilePath;
import hudson.util.DirScanner;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Builds and Uploads the Zip Archive
 */
@SuppressWarnings({"EQ_DOESNT_OVERRIDE_EQUALS","OBL_UNSATISFIED_OBLIGATION"})
public class BuildAndUploadArchive extends DeployerCommand {
    private File localArchive = null;

    @Override
    public boolean perform() throws Exception {
        localArchive = getLocalFileObject(getRootFileObject());

        if (isBlank(getBucketName())) {
            log("bucketName not set. Calling createStorageLocation");

            final CreateStorageLocationResult storageLocation = getAwseb().createStorageLocation();

            log("Using s3 Bucket '%s'", storageLocation.getS3Bucket());

            setBucketName(storageLocation.getS3Bucket());
        }

        setObjectKey(Utils.formatPath("%s/%s-%s.zip", getKeyPrefix(), getApplicationName(),
                getVersionLabel()));

        setS3ObjectPath("s3://" + Utils.formatPath("%s/%s", getBucketName(), getObjectKey()));

        log("Uploading file %s as %s", localArchive.getName(), getS3ObjectPath());

        getS3().putObject(getBucketName(), getObjectKey(), localArchive);

        return false;
    }

    @Override
    public boolean release() throws Exception {
        if (null != localArchive && localArchive.exists()) {
            log("Cleaning up temporary file %s", localArchive.getAbsolutePath());

            FileUtils.deleteQuietly(localArchive);
        }

        return false;
    }

    private File getLocalFileObject(FilePath rootFileObject) throws Exception {
        File resultFile = File.createTempFile("awseb-", ".zip");

        if (!rootFileObject.isDirectory()) {
            log("Root File Object is a file. We assume its a zip file, which is okay.");

            rootFileObject.copyTo(new FileOutputStream(resultFile));
        } else {
            log("Zipping contents of Root File Object (%s) into tmp file %s (includes=%s, excludes=%s)",
                    rootFileObject.getName(), resultFile.getName(),
                    getDeployerConfig().getIncludes(), getDeployerConfig().getExcludes());

            rootFileObject.zip(new FileOutputStream(resultFile),
                    new DirScanner.Glob(getDeployerConfig().getIncludes(),
                            getDeployerConfig().getExcludes()));
        }

        return resultFile;
    }
}
