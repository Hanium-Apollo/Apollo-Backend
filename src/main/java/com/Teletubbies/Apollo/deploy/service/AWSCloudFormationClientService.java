package com.Teletubbies.Apollo.deploy.service;

import com.Teletubbies.Apollo.auth.domain.ApolloUser;
import com.Teletubbies.Apollo.auth.domain.Repo;
import com.Teletubbies.Apollo.auth.repository.RepoRepository;
import com.Teletubbies.Apollo.auth.service.UserService;
import com.Teletubbies.Apollo.credential.domain.Credential;
import com.Teletubbies.Apollo.credential.exception.CredentialException;
import com.Teletubbies.Apollo.credential.repository.CredentialRepository;
import com.Teletubbies.Apollo.deploy.component.AwsClientComponent;
import com.Teletubbies.Apollo.deploy.domain.ApolloDeployService;
import com.Teletubbies.Apollo.deploy.dto.request.DeleteClientDeployRequest;
import com.Teletubbies.Apollo.deploy.dto.request.PostClientDeployRequest;
import com.Teletubbies.Apollo.deploy.dto.response.PostClientDeployResponse;
import com.Teletubbies.Apollo.deploy.exception.DeploymentException;
import com.Teletubbies.Apollo.deploy.repository.AwsServiceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import static com.Teletubbies.Apollo.core.exception.CustomErrorCode.*;

@Service
@Slf4j
public class AWSCloudFormationClientService {
    private final AwsClientComponent awsClientComponent;
    private final RepoRepository repoRepository;
    private final CredentialRepository credentialRepository;
    private final AwsServiceRepository awsServiceRepository;
    private final UserService userService;

    public AWSCloudFormationClientService(AwsClientComponent awsClientComponent, RepoRepository repoRepository, CredentialRepository credentialRepository, AwsServiceRepository awsServiceRepository, UserService userService) {
        this.awsClientComponent = awsClientComponent;
        this.repoRepository = repoRepository;
        this.credentialRepository = credentialRepository;
        this.awsServiceRepository = awsServiceRepository;
        this.userService = userService;
    }

    @Transactional
    public PostClientDeployResponse saveService(Long userId, PostClientDeployRequest request) {
        CloudFormationClient cfClient = awsClientComponent.createCFClient(userId);
        String repoName = request.getRepoName();
        String EndPoint = createClientStack(cfClient, repoName);
        ApolloUser user = userService.getUserById(userId);
        ApolloDeployService apolloDeployService =
                new ApolloDeployService(user, repoName, EndPoint, "client");
        awsServiceRepository.save(apolloDeployService);
        return new PostClientDeployResponse(repoName, "client", EndPoint);
    }

    public String createClientStack(CloudFormationClient cfClient, String repoName) {
        final String templateURL = "https://s3.amazonaws.com/apollo-script/client/cloudformation.yaml";
        Repo repo = repoRepository.findByRepoName(repoName)
                .orElseThrow(() -> new DeploymentException(NOT_FOUND_REPO_ERROR, "해당 레포가 존재하지 않습니다."));
        Long userId = repo.getApolloUser().getId();

        Credential credential = credentialRepository.findByApolloUserId(userId)
                .orElseThrow(() -> new CredentialException(CREDENTIAL_NOT_FOUND_ERROR, "Credential 정보가 없습니다."));

        CreateStackRequest stackRequest = getCreateStackRequest(repoName, templateURL, repo, credential);
        cfClient.createStack(stackRequest);
        Output output = getOutput(cfClient, repoName);
        return output.outputValue();
    }

    private Output getOutput(CloudFormationClient cfClient, String repoName) {
        DescribeStacksRequest describeStacksRequest = DescribeStacksRequest.builder().stackName(repoName).build();
        cfClient.waiter().waitUntilStackCreateComplete(describeStacksRequest);
        DescribeStacksResponse describeStacksResponse = cfClient.describeStacks(describeStacksRequest);
        return describeStacksResponse.stacks().get(0).outputs().get(0);
    }

    private CreateStackRequest getCreateStackRequest(String repoName, String templateURL, Repo repo, Credential credential) {
        return CreateStackRequest
                .builder()
                .templateURL(templateURL)
                .stackName(repoName)
                .parameters(
                        Parameter.builder().parameterKey("RepoName").parameterValue(repo.getRepoName()).build(),
                        Parameter.builder().parameterKey("UserLogin").parameterValue(repo.getOwnerLogin()).build(),
                        Parameter.builder().parameterKey("RepoLocation").parameterValue(repo.getRepoUrl()).build(),
                        Parameter.builder().parameterKey("GithubToken").parameterValue(credential.getGithubOAuthToken()).build(),
                        Parameter.builder().parameterKey("region").parameterValue(credential.getRegion()).build()
                )
                .capabilitiesWithStrings("CAPABILITY_IAM")
                .build();
    }

    @Transactional
    public void deleteClientStack(Long userId, DeleteClientDeployRequest request) {
        CloudFormationClient cfClient = awsClientComponent.createCFClient(userId);
        S3Client s3Client = awsClientComponent.createS3Client(userId);
        ApolloDeployService service = awsServiceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new DeploymentException(NOT_FOUND_SERVICE_ERROR, "해당 서비스가 존재하지 않습니다."));
        String stackName = service.getStackName();
        String bucketName = getBucketName(cfClient, stackName);

        if (bucketName != null) {
            deleteS3Bucket(s3Client, bucketName);
            deleteCloudFormationStack(cfClient, stackName);
        } else {
            log.info("버킷이 존재하지 않습니다.");
        }
        if (!service.getApolloUser().getId().equals(userId))
            throw new IllegalArgumentException("서비스 작성자와 삭제자가 일치하지 않습니다.");
        awsServiceRepository.delete(service);
        log.info("Delete service: " + stackName + " successfully");
    }


    private void deleteS3Bucket(S3Client s3Client, String bucketName) {
        try {
            emptyS3Bucket(s3Client, bucketName);
            DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(bucketName).build();
            s3Client.deleteBucket(deleteBucketRequest);
            log.info("다음 버킷을 삭제했습니다.: " + bucketName);
        } catch (Exception e) {
            log.info("버킷 삭제중에 에러가 발생했습니다.: " + e.getMessage());
        }
    }

    private void emptyS3Bucket(S3Client s3Client, String bucketName) {
        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder().bucket(bucketName).build();
        ListObjectsV2Response listObjectsV2Response;

        do {
            listObjectsV2Response = s3Client.listObjectsV2(listObjectsV2Request);
            for (S3Object s3Object : listObjectsV2Response.contents()) {
                DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder().bucket(bucketName).key(s3Object.key()).build();
                s3Client.deleteObject(deleteObjectRequest);
            }
            listObjectsV2Request = listObjectsV2Request.toBuilder().continuationToken(listObjectsV2Response.nextContinuationToken()).build();
        } while (listObjectsV2Response.isTruncated());
    }

    private void deleteCloudFormationStack(CloudFormationClient cfClient, String stackName) {
        try {
            DeleteStackRequest deleteStackRequest = DeleteStackRequest.builder().stackName(stackName).build();
            cfClient.deleteStack(deleteStackRequest);
            log.info("Delete stack: " + stackName + " successfully");
        } catch (Exception e) {
            log.info("스택 삭제중에 에러가 발생했습니다.: " + e.getMessage());
        }
    }

    private String getBucketName(CloudFormationClient cfClient, String stackName) {
        try {
            DescribeStackResourcesRequest request = DescribeStackResourcesRequest.builder()
                    .stackName(stackName)
                    .build();
            DescribeStackResourcesResponse response = cfClient.describeStackResources(request);
            for (StackResource stackResource : response.stackResources()) {
                if ("AWS::S3::Bucket".equals(stackResource.resourceType())) {
                    return stackResource.physicalResourceId();
                }
            }
        } catch (Exception e) {
            log.info("Error occurred while fetching S3 bucket for stack: " + e.getMessage());
        }
        return null;
    }
}
