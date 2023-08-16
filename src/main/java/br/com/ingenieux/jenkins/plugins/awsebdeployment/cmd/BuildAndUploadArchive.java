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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.FilePath;
import hudson.util.DirScanner;
import java.nio.file.Files;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Builds and Uploads the Zip Archive
 */
@SuppressFBWarnings({"EQ_DOESNT_OVERRIDE_EQUALS", "OBL_UNSATISFIED_OBLIGATION"})
public class BuildAndUploadArchive extends DeployerCommand {
    private File localArchive = null;

    @Override
    public boolean perform() throws Exception {
        localArchive = getLocalFileObject(getRootFileObject());

      log("Using archive '%s'", localArchive.getAbsolutePath());

        if (isBlank(c.config.getBucketName())) {
            log("bucketName not set. Calling createStorageLocation");

            final CreateStorageLocationResult storageLocation = getAwseb().createStorageLocation();

            log("Using s3 Bucket '%s'", storageLocation.getS3Bucket());

            c.config.setBucketName(storageLocation.getS3Bucket());
        }

        setObjectKey(Utils.formatPath("%s/%s-%s.zip", c.config.getKeyPrefix(), c.config.getApplicationName(),
                getVersionLabel()));

        setS3ObjectPath("s3://" + Utils.formatPath("%s/%s", c.config.getBucketName(), getObjectKey()));

        log("Uploading file %s as %s", localArchive.getName(), getS3ObjectPath());

        getS3().putObject(c.config.getBucketName(), getObjectKey(), localArchive);

        return false;
    }

    @Override
    public boolean release() {
        if (null != localArchive && localArchive.exists()) {
            log("Cleaning up temporary file %s", localArchive.getAbsolutePath());

            FileUtils.deleteQuietly(localArchive);
        }

        return false;
    }

    private File getLocalFileObject(FilePath rootFileObject) throws Exception {
        File resultFile = Files.createTempFile("awseb-", ".zip");

        if (!rootFileObject.isDirectory()) {
            log("Root File Object is a file. We assume its a zip file, which is okay.");

            rootFileObject.copyTo(new FileOutputStream(resultFile));
        } else {
            log("Zipping contents of Root File Object (%s) into tmp file %s (includes=%s, excludes=%s)",
                    rootFileObject.getName(), resultFile.getName(),
                    getConfig().getIncludes(), getConfig().getExcludes());

            rootFileObject.zip(new FileOutputStream(resultFile),
                    new DirScanner.Glob(getConfig().getIncludes(),
                            getConfig().getExcludes()));
        }

        return resultFile;
    }
}
