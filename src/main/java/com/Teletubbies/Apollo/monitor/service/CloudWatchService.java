package com.Teletubbies.Apollo.monitor.service;

import com.Teletubbies.Apollo.deploy.component.AwsClientComponent;
import com.Teletubbies.Apollo.monitor.component.InstantAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class CloudWatchService {
    private final AwsClientComponent awsClientComponent;

    public CloudWatchService(AwsClientComponent awsClientComponent) {
        this.awsClientComponent = awsClientComponent;
    }
    public String getCloudWatchMetrics(String namespace, Long userId) {
        CloudWatchClient cloudWatchClient = awsClientComponent.createCloudWatchClient(userId);
        List<Dimension> dimensions = new ArrayList<>();
        dimensions.add(Dimension.builder().name("ClusterName").value("Apollo-Server-Cluster").build());
        dimensions.add(Dimension.builder().name("ServiceName").value("Apollo-Server-Service").build());
        Instant startInstant = Instant.now().minus(Duration.ofHours(1));
        Instant endInstant = Instant.now();

        MetricDataQuery cpuQuery = MetricDataQuery.builder()
                .id("cpuQuery")
                .metricStat(
                        MetricStat.builder()
                                .metric(
                                        Metric.builder()
                                                .metricName("CPUUtilization")
                                                .namespace(namespace)
                                                .dimensions(dimensions)
                                                .build()
                                )
                                .period(300)
                                .stat("Average")
                                .build()
                )
                .returnData(true)
                .build();
        MetricDataQuery memoryQuery = MetricDataQuery.builder()
                .id("memoryQuery")
                .metricStat(
                        MetricStat.builder()
                                .metric(
                                        Metric.builder()
                                                .metricName("MemoryUtilization")
                                                .namespace(namespace)
                                                .dimensions(dimensions)
                                                .build()
                                )
                                .period(300)
                                .stat("Average")
                                .build()
                )
                .returnData(true)
                .build();
        GetMetricDataRequest request = GetMetricDataRequest.builder()
                .startTime(startInstant)
                .endTime(endInstant)
                .metricDataQueries(cpuQuery, memoryQuery)
                .build();
        GetMetricDataResponse response = cloudWatchClient.getMetricData(request);
        String jsonMetric = convertMetricDataToJson(response);
        return jsonMetric;
    }

    private String convertMetricDataToJson(GetMetricDataResponse response) {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Instant.class, new InstantAdapter());
        Gson gson = builder.create();
        return gson.toJson(response.metricDataResults());
    }
}