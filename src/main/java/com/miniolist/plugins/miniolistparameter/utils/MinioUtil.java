package com.miniolist.plugins.miniolistparameter.utils;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.HostnameRequirement;
import hudson.security.ACL;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import jenkins.model.Jenkins;

import javax.security.auth.login.CredentialNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.sort;

public class MinioUtil {

    /**
     * 传递 minio info
     * 返回 minio client
     */
    public static final MinioClient getClient(String host, String credentialsId) throws CredentialNotFoundException, IOException, NoSuchAlgorithmException, InvalidKeyException {

        StandardUsernamePasswordCredentials credentials = getCredentials(credentialsId);

        return MinioClient.builder()
                .endpoint(host)
                .credentials(credentials.getUsername(), credentials.getPassword().getPlainText())
                .build();
    }

    /**
     * 传递 minio path info
     * 返回 map对象
     */
    public static Map<String, String> listMinio(MinioClient client, String backet, String project, String size, Boolean debug) throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        Map<String, String> paramList = new LinkedHashMap<String, String>();

        int lime_size = Integer.valueOf(size).intValue();

        try {


            // Lists objects information recursively.
            Iterable<Result<Item>> results =
                    client.listObjects(
                            ListObjectsArgs.builder().bucket(backet).prefix(project).recursive(true).build());

            List<String> list = StreamSupport.stream(results.spliterator(), false).map(temp -> {
                try {
                    return temp.get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }).sorted(Comparator.comparing(Item::lastModified).reversed()).map(temp -> temp.objectName().split("/")[1]).limit(lime_size).collect(Collectors.toList());

            list.forEach(item -> {
                paramList.put(item, item);
            });


        } catch (Exception e) {
            System.out.println("Error occurred: " + e);
            paramList.clear();
            paramList.put(e.getMessage(), e.getMessage());
        }

        if (debug == true) {
            System.out.println("minio list: " + paramList);
        }
        return paramList;

    }

    /**
     * 传递 认证ID
     * 返回 认证对象
     */
    public static StandardUsernamePasswordCredentials getCredentials(String credentialsId) {

        StandardUsernamePasswordCredentials creds = CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        StandardUsernamePasswordCredentials.class,
                        Jenkins.getInstance(), ACL.SYSTEM,
                        new HostnameRequirement("")),
                CredentialsMatchers.withId(credentialsId));

        return creds;
    }


}
