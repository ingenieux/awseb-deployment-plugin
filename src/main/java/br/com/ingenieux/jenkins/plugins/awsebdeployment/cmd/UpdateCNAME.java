package br.com.ingenieux.jenkins.plugins.awsebdeployment.cmd;

import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.route53.model.Change;
import com.amazonaws.services.route53.model.ChangeAction;
import com.amazonaws.services.route53.model.ChangeBatch;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ResourceRecord;
import com.amazonaws.services.route53.model.ResourceRecordSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Update CNAME
 */
public class UpdateCNAME extends DeployerCommand {

    @Override
    public boolean perform() throws Exception {
        final DescribeEnvironmentsRequest req = new DescribeEnvironmentsRequest().
                withApplicationName(getApplicationName()).
                withEnvironmentIds(getEnvironmentId()).
                withIncludeDeleted(false);

        final DescribeEnvironmentsResult result = getAwseb().describeEnvironments(req);

        if (1 != result.getEnvironments().size()) {
            log("Environment w/ environmentId '%s' not found. Aborting.", getEnvironmentId());

            return true;
        }

        String newRecordValue = result.getEnvironments().get(0).getCNAME();

        ResourceRecord resourceRecord = new ResourceRecord()
                .withValue(newRecordValue);

        List<ResourceRecord> resourceRecords = new ArrayList<ResourceRecord>();
        resourceRecords.add(resourceRecord);

        ResourceRecordSet resourceRecordSet = new ResourceRecordSet()
                .withName(getRoute53DomainName())
                .withTTL(getRoute53RecordTTL())
                .withType(getRoute53RecordType())
                .withResourceRecords(resourceRecords);

        Change change = new Change()
                .withAction(ChangeAction.UPSERT)
                .withResourceRecordSet(resourceRecordSet);

        List<Change> changes = new ArrayList<Change>();
        changes.add(change);

        ChangeBatch changeBatch = new ChangeBatch()
                .withChanges(changes);

        final ChangeResourceRecordSetsRequest crrsReq = new ChangeResourceRecordSetsRequest()
                .withHostedZoneId(getRoute53HostedZoneId())
                .withChangeBatch(changeBatch);

        getRoute53().changeResourceRecordSets(crrsReq);

        log("Pointed %s to %s", newRecordValue, getRoute53DomainName());

        return false;
    }
}