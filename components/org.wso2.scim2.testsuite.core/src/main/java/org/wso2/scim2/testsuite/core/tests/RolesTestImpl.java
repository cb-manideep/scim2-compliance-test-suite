/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
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

package org.wso2.scim2.testsuite.core.tests;

import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.charon3.core.encoder.JSONDecoder;
import org.wso2.charon3.core.exceptions.BadRequestException;
import org.wso2.charon3.core.exceptions.CharonException;
import org.wso2.charon3.core.exceptions.InternalErrorException;
import org.wso2.charon3.core.objects.Group;
import org.wso2.charon3.core.objects.Role;
import org.wso2.charon3.core.objects.User;
import org.wso2.charon3.core.schema.SCIMResourceSchemaManager;
import org.wso2.charon3.core.schema.SCIMResourceTypeSchema;
import org.wso2.scim2.testsuite.core.entities.TestResult;
import org.wso2.scim2.testsuite.core.exception.ComplianceException;
import org.wso2.scim2.testsuite.core.exception.GeneralComplianceException;
import org.wso2.scim2.testsuite.core.httpclient.HTTPClient;
import org.wso2.scim2.testsuite.core.protocol.ComplianceTestMetaDataHolder;
import org.wso2.scim2.testsuite.core.protocol.ComplianceUtils;
import org.wso2.scim2.testsuite.core.tests.common.ResponseValidateTests;
import org.wso2.scim2.testsuite.core.tests.model.RequestPath;
import org.wso2.scim2.testsuite.core.utils.ComplianceConstants;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

/**
 * Implementation of Roles test cases.
 */
public class RolesTestImpl implements ResourceType {

    private final ComplianceTestMetaDataHolder complianceTestMetaDataHolder;
    private final String url;

    /**
     * Initialize.
     *
     * @param complianceTestMetaDataHolder Stores data required to run tests.
     */
    public RolesTestImpl(ComplianceTestMetaDataHolder complianceTestMetaDataHolder) {

        this.complianceTestMetaDataHolder = complianceTestMetaDataHolder;

        url = complianceTestMetaDataHolder.getUrl() +
                ComplianceConstants.TestConstants.ROLES_ENDPOINT;
    }

    /**
     * Create test users for test cases.
     *
     * @param noOfUsers Specify the number of users needs to create.
     * @return userIDS of created users.
     * @throws ComplianceException        Constructed new exception with the specified detail message.
     * @throws GeneralComplianceException General exceptions.
     */
    private ArrayList<String> createTestsUsers(String noOfUsers) throws ComplianceException,
            GeneralComplianceException {

        String url = complianceTestMetaDataHolder.getUrl() +
                ComplianceConstants.TestConstants.USERS_ENDPOINT;

        ArrayList<String> definedUsers = new ArrayList<>();
        ArrayList<String> userIDs = new ArrayList<>();

        if (noOfUsers.equals("One")) {
            definedUsers.add(ComplianceConstants.DefinedInstances.defineUser);
        } else if (noOfUsers.equals("Many")) {
            definedUsers.add(ComplianceConstants.DefinedInstances.definedUser1);
            definedUsers.add(ComplianceConstants.DefinedInstances.definedUser2);
            definedUsers.add(ComplianceConstants.DefinedInstances.definedUser3);
            definedUsers.add(ComplianceConstants.DefinedInstances.definedUser4);
            definedUsers.add(ComplianceConstants.DefinedInstances.definedUser5);
        }

        HttpPost method = new HttpPost(url);
        // Create users.
        HttpClient client = HTTPClient.getHttpClient();
        HTTPClient.setAuthorizationHeader(complianceTestMetaDataHolder, method);
        method.setHeader(ComplianceConstants.RequestCodeConstants.ACCEPT,
                ComplianceConstants.RequestCodeConstants.APPLICATION_JSON);
        method.setHeader(ComplianceConstants.RequestCodeConstants.CONTENT_TYPE,
                ComplianceConstants.RequestCodeConstants.APPLICATION_JSON);
        HttpResponse response = null;
        String responseString = StringUtils.EMPTY;
        StringBuilder headerString = new StringBuilder(StringUtils.EMPTY);
        String responseStatus;
        ArrayList<String> subTests = new ArrayList<>();
        for (String definedUser : definedUsers) {
            long startTime = System.currentTimeMillis();
            try {
                // Create Users.
                HttpEntity entity = new ByteArrayEntity(definedUser.getBytes(StandardCharsets.UTF_8));
                method.setEntity(entity);
                response = client.execute(method);
                // Read the response body.
                responseString = new BasicResponseHandler().handleResponse(response);
                responseStatus = String.valueOf(response.getStatusLine().getStatusCode());
                if (responseStatus.equals("201")) {
                    // Obtain the schema corresponding to the user.
                    SCIMResourceTypeSchema schema = SCIMResourceSchemaManager.getInstance().getUserResourceSchema();
                    JSONDecoder jsonDecoder = new JSONDecoder();
                    User user;
                    try {
                        user = jsonDecoder.decodeResource(responseString, schema, new User());
                    } catch (BadRequestException | CharonException | InternalErrorException e) {
                        long stopTime = System.currentTimeMillis();
                        throw new GeneralComplianceException(new TestResult(TestResult.ERROR, "List Users",
                                "Could not decode the server response of users create.",
                                ComplianceUtils.getWire(method, responseString, headerString.toString(), responseStatus,
                                        subTests), stopTime - startTime));
                    }
                    userIDs.add(user.getId());
                }
            } catch (Exception e) {
                // Read the response body.
                assert response != null;
                Header[] headers = response.getAllHeaders();
                for (Header header : headers) {
                    headerString.append(String.format("%s : %s \n", header.getName(), header.getValue()));
                }
                responseStatus = response.getStatusLine().getStatusCode() + " "
                        + response.getStatusLine().getReasonPhrase();
                long stopTime = System.currentTimeMillis();
                throw new GeneralComplianceException(new TestResult(TestResult.ERROR, "List Users",
                        "Could not create default users at url " + url,
                        ComplianceUtils.getWire(method, responseString, headerString.toString(), responseStatus,
                                subTests), stopTime - startTime));
            }
        }
        return userIDs;
    }

    /**
     * Create test groups for test cases.
     *
     * @param userIDs    Array of user ids to use as members in groups.
     * @param noOfGroups Specify the number of groups needs to create.
     * @return groupIDs of created groups.
     * @throws ComplianceException        Constructed new exception with the specified detail message.
     * @throws GeneralComplianceException General exceptions.
     */
    private ArrayList<String> createTestsGroups(ArrayList<String> userIDs, String noOfGroups) throws
            ComplianceException, GeneralComplianceException {

        String url = complianceTestMetaDataHolder.getUrl() +
                ComplianceConstants.TestConstants.GROUPS_ENDPOINT;

        ArrayList<String> groupIDs = new ArrayList<>();
        ArrayList<String> definedGroups = new ArrayList<>();

        if (noOfGroups.equals("One")) {
            definedGroups.add("{\"schemas\":[\"urn:ietf:params:scim:schemas:core:2.0:Group\"]," +
                    "\"displayName\":\"XwLtOP23\",\"members\":[{\"value\":\"" + userIDs.get(0) + "\",\"displayName" +
                    "\":\"loginUser1\",\"$ref\":\"" + complianceTestMetaDataHolder.getUrl() +
                    ComplianceConstants.TestConstants.USERS_ENDPOINT + "/" + userIDs.get(0) + "\"}," +
                    "{\"value\":\"" + userIDs.get(1) + "\",\"displayName\":\"loginUser2\"},{\"value\":\"" +
                    userIDs.get(2) + "\",\"displayName\":\"loginUser3\"},{\"value\":\"" + userIDs.get(3) +
                    "\",\"displayName" + "\":\"loginUser4" + "\"}," +
                    "{\"value\":\"" + userIDs.get(4) + "\",\"displayName\":\"loginUser5\"}]}");
        } else if (noOfGroups.equals("Many")) {
            definedGroups.add("{\"displayName\": \"EYtXcD21\"}");
            definedGroups.add("{\"displayName\": \"BktqER22\"}");
            definedGroups.add("{\"displayName\": \"ZwLtOP23\"}");
            definedGroups.add("{\"schemas\":[\"urn:ietf:params:scim:schemas:core:2.0:Group\"]," +
                    "\"displayName\":\"XwLtOP23\",\"members\":[{\"value\":\"" + userIDs.get(0) + "\",\"displayName" +
                    "\":\"loginUser1\"}," + "{\"value\":\"" + userIDs.get(1) + "\",\"displayName\":\"loginUser2\"}," +
                    "{\"value\":\"" + userIDs.get(2) + "\",\"displayName\":\"loginUser3\"},{\"value\":\"" +
                    userIDs.get(3) + "\",\"displayName" + "\":\"loginUser4" + "\"}," +
                    "{\"value\":\"" + userIDs.get(4) + "\",\"displayName\":\"loginUser5\"}]}");
        }

        HttpPost method = new HttpPost(url);
        // Create groups.
        HttpClient client = HTTPClient.getHttpClient();
        HTTPClient.setAuthorizationHeader(complianceTestMetaDataHolder, method);
        method.setHeader(ComplianceConstants.RequestCodeConstants.ACCEPT,
                ComplianceConstants.RequestCodeConstants.APPLICATION_JSON);
        method.setHeader(ComplianceConstants.RequestCodeConstants.CONTENT_TYPE,
                ComplianceConstants.RequestCodeConstants.APPLICATION_JSON);
        HttpResponse response = null;
        String responseString = StringUtils.EMPTY;
        StringBuilder headerString = new StringBuilder();
        String responseStatus;
        ArrayList<String> subTests = new ArrayList<>();
        for (String definedGroup : definedGroups) {
            long startTime = System.currentTimeMillis();
            try {
                // Create the group.
                HttpEntity entity = new ByteArrayEntity(definedGroup.getBytes(StandardCharsets.UTF_8));
                method.setEntity(entity);
                response = client.execute(method);
                // Read the response body.
                responseString = new BasicResponseHandler().handleResponse(response);
                responseStatus = String.valueOf(response.getStatusLine().getStatusCode());
                if (responseStatus.equals("201")) {
                    // Obtain the schema corresponding to group.
                    SCIMResourceTypeSchema schema = SCIMResourceSchemaManager.getInstance().getGroupResourceSchema();
                    JSONDecoder jsonDecoder = new JSONDecoder();
                    Group group;
                    try {
                        group = jsonDecoder.decodeResource(responseString, schema, new Group());
                    } catch (BadRequestException | CharonException | InternalErrorException e) {
                        long stopTime = System.currentTimeMillis();
                        throw new GeneralComplianceException(new TestResult(TestResult.ERROR, "List Groups",
                                "Could not decode the server response of groups create.",
                                ComplianceUtils.getWire(method, responseString, headerString.toString(), responseStatus,
                                        subTests), stopTime - startTime));
                    }
                    groupIDs.add(group.getId());
                }
            } catch (Exception e) {
                // Read the response body.
                assert response != null;
                Header[] headers = response.getAllHeaders();
                for (Header header : headers) {
                    headerString.append(String.format("%s : %s \n", header.getName(), header.getValue()));
                }
                responseStatus = response.getStatusLine().getStatusCode() + " "
                        + response.getStatusLine().getReasonPhrase();
                long stopTime = System.currentTimeMillis();
                throw new GeneralComplianceException(new TestResult(TestResult.ERROR, "List Groups",
                        "Could not create default groups at url " + url,
                        ComplianceUtils.getWire(method, responseString, headerString.toString(), responseStatus,
                                subTests), stopTime - startTime));
            }
        }
        return groupIDs;
    }

    /**
     * Create test roles for test cases.
     *
     * @param userIDs   Array of user ids to use as members in groups.
     * @param groupIDs  Array of group ids to use as in roles.
     * @param noOfRoles Specify the number of roles needs to create.
     * @return roleIDs of created roles.
     * @throws ComplianceException        Constructed new exception with the specified detail message.
     * @throws GeneralComplianceException General exceptions.
     */
    private ArrayList<String> createTestsRoles(ArrayList<String> userIDs, ArrayList<String> groupIDs,
                                               String noOfRoles) throws
            ComplianceException, GeneralComplianceException {

        ArrayList<String> roleIDs = new ArrayList<>();
        ArrayList<String> definedRoles = new ArrayList<>();

        if (noOfRoles.equals("One")) {
            definedRoles.add("{\n" +
                    "  \"schemas\": [\n" +
                    "    \"urn:ietf:params:scim:schemas:extension:2.0:Role\"\n" +
                    "  ],\n" +
                    "  \"displayName\": \"loginRole1\",\n" +
                    "  \"users\": [\n" +
                    "    {\n" +
                    "      \"value\": \"" + userIDs.get(0) + "\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"groups\": [\n" +
                    "    {\n" +
                    "      \"value\": \"" + groupIDs.get(0) + "\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"permissions\": [\n" +
                    "    \"/permission/admin/login\"\n" +
                    "  ]\n" +
                    "}");
        } else if (noOfRoles.equals("Many")) {
            definedRoles.add("{\"displayName\": \"loginRole1\"}");
            definedRoles.add("{\"displayName\": \"loginRole2\"}");
            definedRoles.add("{\"displayName\": \"loginRole3\"}");
            definedRoles.add("{\n" +
                    "  \"schemas\": [\n" +
                    "    \"urn:ietf:params:scim:schemas:extension:2.0:Role\"\n" +
                    "  ],\n" +
                    "  \"displayName\": \"loginRole4\",\n" +
                    "  \"users\": [\n" +
                    "    {\n" +
                    "      \"value\": \"" + userIDs.get(0) + "\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"groups\": [\n" +
                    "    {\n" +
                    "      \"value\": \"" + groupIDs.get(0) + "\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"permissions\": [\n" +
                    "    \"/permission/admin/login\"\n" +
                    "  ]\n" +
                    "}");
        }

        HttpPost method = new HttpPost(url);
        // Create groups.
        HttpClient client = HTTPClient.getHttpClient();
        HTTPClient.setAuthorizationHeader(complianceTestMetaDataHolder, method);
        method.setHeader(ComplianceConstants.RequestCodeConstants.ACCEPT,
                ComplianceConstants.RequestCodeConstants.APPLICATION_JSON);
        method.setHeader(ComplianceConstants.RequestCodeConstants.CONTENT_TYPE,
                ComplianceConstants.RequestCodeConstants.APPLICATION_JSON);
        HttpResponse response = null;
        String responseString = StringUtils.EMPTY;
        StringBuilder headerString = new StringBuilder();
        String responseStatus;
        ArrayList<String> subTests = new ArrayList<>();
        for (String definedRole : definedRoles) {
            long startTime = System.currentTimeMillis();
            try {
                // Create the role.
                HttpEntity entity = new ByteArrayEntity(definedRole.getBytes(StandardCharsets.UTF_8));
                method.setEntity(entity);
                response = client.execute(method);
                // Read the response body.
                responseString = new BasicResponseHandler().handleResponse(response);
                responseStatus = String.valueOf(response.getStatusLine().getStatusCode());
                if (responseStatus.equals("201")) {
                    // Obtain the schema corresponding to role.
                    SCIMResourceTypeSchema schema = SCIMResourceSchemaManager.getInstance().getRoleResourceSchema();
                    JSONDecoder jsonDecoder = new JSONDecoder();
                    Role role;
                    try {
                        role = jsonDecoder.decodeResource(responseString, schema, new Role());
                    } catch (BadRequestException | CharonException | InternalErrorException e) {
                        long stopTime = System.currentTimeMillis();
                        throw new GeneralComplianceException(new TestResult(TestResult.ERROR, "List Groups",
                                "Could not decode the server response of groups create.",
                                ComplianceUtils.getWire(method, responseString, headerString.toString(), responseStatus,
                                        subTests), stopTime - startTime));
                    }
                    roleIDs.add(role.getId());
                }
            } catch (Exception e) {
                // Read the response body.
                assert response != null;
                Header[] headers = response.getAllHeaders();
                for (Header header : headers) {
                    headerString.append(String.format("%s : %s \n", header.getName(), header.getValue()));
                }
                responseStatus = response.getStatusLine().getStatusCode() + " "
                        + response.getStatusLine().getReasonPhrase();
                long stopTime = System.currentTimeMillis();
                throw new GeneralComplianceException(new TestResult(TestResult.ERROR, "List Groups",
                        "Could not create default groups at url " + url,
                        ComplianceUtils.getWire(method, responseString, headerString.toString(), responseStatus,
                                subTests), stopTime - startTime));
            }
        }
        return roleIDs;
    }

    /**
     * This method cleans up resources.
     *
     * @param location Resource location.
     * @param testName Related test case name.
     * @throws GeneralComplianceException General exceptions.
     * @throws ComplianceException        Constructed new exception with the specified detail message.
     */
    private void cleanUp(String location, String testName) throws GeneralComplianceException, ComplianceException {

        long startTime = System.currentTimeMillis();
        String deleteUserURL;

        switch (testName) {
            case "User": {
                String url = complianceTestMetaDataHolder.getUrl() +
                        ComplianceConstants.TestConstants.USERS_ENDPOINT;
                deleteUserURL = url + "/" + location;
                break;
            }
            case "Group": {
                String url = complianceTestMetaDataHolder.getUrl() +
                        ComplianceConstants.TestConstants.GROUPS_ENDPOINT;
                deleteUserURL = url + "/" + location;
                break;
            }
            case "Role": {
                String url = complianceTestMetaDataHolder.getUrl() +
                        ComplianceConstants.TestConstants.ROLES_ENDPOINT;
                deleteUserURL = url + "/" + location;
                break;
            }
            default:
                deleteUserURL = location;
                break;
        }

        HttpDelete method = new HttpDelete(deleteUserURL);
        HttpClient client = HTTPClient.getHttpClient();
        HTTPClient.setAuthorizationHeader(complianceTestMetaDataHolder, method);
        method.setHeader(ComplianceConstants.RequestCodeConstants.ACCEPT,
                ComplianceConstants.RequestCodeConstants.APPLICATION_JSON);
        HttpResponse response = null;
        String responseString = StringUtils.EMPTY;
        StringBuilder headerString = new StringBuilder(StringUtils.EMPTY);
        String responseStatus;
        ArrayList<String> subTests = new ArrayList<>();
        try {
            response = client.execute(method);
            // Read the response body.
            responseString = new BasicResponseHandler().handleResponse(response);
            // Get all headers.
            Header[] headers = response.getAllHeaders();
            for (Header header : headers) {
                headerString.append(String.format("%s : %s \n", header.getName(), header.getValue()));
            }
            responseStatus = response.getStatusLine().getStatusCode() + " "
                    + response.getStatusLine().getReasonPhrase();
        } catch (Exception e) {
            /*
             Read the response body.
             Get all headers.
             */
            assert response != null;
            Header[] headers = response.getAllHeaders();
            for (Header header : headers) {
                headerString.append(String.format("%s : %s \n", header.getName(), header.getValue()));
            }
            responseStatus = response.getStatusLine().getStatusCode() + " "
                    + response.getStatusLine().getReasonPhrase();
            long stopTime = System.currentTimeMillis();
            throw new GeneralComplianceException(new TestResult(TestResult.ERROR, testName,
                    "Could not delete the default user at url " + url,
                    ComplianceUtils.getWire(method, responseString, headerString.toString(), responseStatus,
                            subTests), stopTime - startTime));
        }
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_NO_CONTENT) {
            long stopTime = System.currentTimeMillis();
            throw new GeneralComplianceException(new TestResult(TestResult.ERROR, testName,
                    "Could not delete the default user at url " + url,
                    ComplianceUtils.getWire(method, responseString, headerString.toString(), responseStatus,
                            subTests), stopTime - startTime));
        }
    }

    /**
     * This checks whether the given array list of roles are in sorted order with respect to role id.
     *
     * @param returnedRoles Array of roles to get checked whether they are sorted or not.
     * @return true or false.
     * @throws CharonException Exception by charon library.
     */
    private boolean isRoleListSorted(ArrayList<Role> returnedRoles) throws CharonException {

        boolean sorted = true;
        for (int i = 1; i < returnedRoles.size(); i++) {
            if (returnedRoles.get(i - 1).getId().compareTo(returnedRoles.get(i).getId()) > 0) {
                sorted = false;
            }
        }
        return sorted;
    }

    /**
     * Generating unique numbers.
     *
     * @return unique number.
     */
    private static String generateUniqueID() {

        return UUID.randomUUID().toString();
    }

    /**
     * Get role tests.
     *
     * @return testResults Array containing test results.
     * @throws GeneralComplianceException General exceptions.
     * @throws ComplianceException        Constructed new exception with the specified detail message.
     */
    @Override
    public ArrayList<TestResult> getMethodTest() throws GeneralComplianceException, ComplianceException {

        ArrayList<TestResult> testResults = new ArrayList<>();
        ArrayList<String> userIDs;
        ArrayList<String> groupIDs;
        ArrayList<String> roleIDs;
        // Create 5 test users to assign for groups.
        userIDs = createTestsUsers("Many");
        // Create test groups with users.
        groupIDs = createTestsGroups(userIDs, "Many");
        // Create test roles with users and groups.
        roleIDs = createTestsRoles(userIDs, groupIDs, "Many");

        RequestPath[] requestPaths;

        RequestPath requestPath1 = new RequestPath();
        requestPath1.setUrl(StringUtils.EMPTY);
        requestPath1.setTestCaseName("List roles");

        RequestPath requestPath2 = new RequestPath();
        requestPath2.setUrl("?filter=displayName+eq+loginRole1");
        requestPath2.setTestCaseName("Get roles with Filter");
        try {
            requestPath2.setTestSupported(complianceTestMetaDataHolder.getScimServiceProviderConfig().
                    getFilterSupported());
        } catch (Exception e) {
            requestPath2.setTestSupported(true);
        }

        RequestPath requestPath3 = new RequestPath();
        requestPath3.setUrl("?startIndex=1&count=2");
        requestPath3.setTestCaseName("Get roles with Pagination");

        RequestPath requestPath4 = new RequestPath();
        requestPath4.setUrl("?sortBy=id&sortOrder=ascending");
        requestPath4.setTestCaseName("Sort test");
        try {
            requestPath4.setTestSupported(complianceTestMetaDataHolder.getScimServiceProviderConfig().
                    getSortSupported());
        } catch (Exception e) {
            requestPath4.setTestSupported(true);
        }

        RequestPath requestPath5 = new RequestPath();
        requestPath5.setUrl("?filter=displayName+eq+loginRole1&startIndex=1&count=1");
        requestPath5.setTestCaseName("Filter with pagination test");
        try {
            requestPath5.setTestSupported(complianceTestMetaDataHolder.getScimServiceProviderConfig().
                    getFilterSupported());
        } catch (Exception e) {
            requestPath5.setTestSupported(true);
        }

        RequestPath requestPath6 = new RequestPath();
        requestPath6.setUrl("?startIndex=-1&count=2");
        requestPath6.setTestCaseName("Get roles having negative number as index");

        RequestPath requestPath7 = new RequestPath();
        requestPath7.setUrl("?count=2");
        requestPath7.setTestCaseName("Get roles without index and only using count");

        // This array hold the sub tests details.
        requestPaths = new RequestPath[]{requestPath1, requestPath2, requestPath3, requestPath4, requestPath5,
                requestPath7};

        for (RequestPath requestPath : requestPaths) {
            long startTime = System.currentTimeMillis();
            String requestUrl = url + requestPath.getUrl();
            HttpGet method = new HttpGet(requestUrl);
            HttpClient client = HTTPClient.getHttpClient();
            HTTPClient.setAuthorizationHeader(complianceTestMetaDataHolder, method);
            method.setHeader(ComplianceConstants.RequestCodeConstants.ACCEPT,
                    ComplianceConstants.RequestCodeConstants.APPLICATION_JSON);
            HttpResponse response = null;
            String responseString = StringUtils.EMPTY;
            StringBuilder headerString = new StringBuilder(StringUtils.EMPTY);
            String responseStatus;
            int startIndex;
            int count;
            ArrayList<String> subTests = new ArrayList<>();
            boolean errorOccur = false;
            try {
                response = client.execute(method);
                // Read the response body.
                responseString = new BasicResponseHandler().handleResponse(response);
                // Get all headers.
                Header[] headers = response.getAllHeaders();
                for (Header header : headers) {
                    headerString.append(String.format("%s : %s \n", header.getName(), header.getValue()));
                }
                responseStatus = response.getStatusLine().getStatusCode() + " " +
                        response.getStatusLine().getReasonPhrase();
            } catch (Exception e) {
                /*
                 Read the response body.
                 Get all headers.
                 */
                assert response != null;
                Header[] headers = response.getAllHeaders();
                for (Header header : headers) {
                    headerString.append(String.format("%s : %s \n", header.getName(), header.getValue()));
                }
                responseStatus = response.getStatusLine().getStatusCode() + " "
                        + response.getStatusLine().getReasonPhrase();
                if (requestPath.getTestSupported() &&
                        response.getStatusLine().getStatusCode() != HttpStatus.SC_NOT_IMPLEMENTED) {
                    long stopTime = System.currentTimeMillis();
                    testResults.add(new TestResult(TestResult.ERROR, requestPath.getTestCaseName(),
                            "Could not list the roles at url " + url,
                            ComplianceUtils.getWire(method, responseString, headerString.toString(), responseStatus,
                                    subTests), stopTime - startTime));
                    continue;
                }
            }
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                // Obtain the schema corresponding to group.
                SCIMResourceTypeSchema schema = SCIMResourceSchemaManager.getInstance().getRoleResourceSchema();
                JSONDecoder jsonDecoder = new JSONDecoder();
                JSONObject jsonObjResponse;
                ArrayList<Role> roleList = new ArrayList<>();
                try {
                    JSONObject jsonObj = new JSONObject(responseString);
                    jsonObjResponse = jsonObj;
                    JSONArray rolesArray = jsonObj.getJSONArray("Resources");
                    startIndex = (int) jsonObjResponse.get("startIndex");
                    count = (int) jsonObjResponse.get("totalResults");
                    JSONObject tmp;
                    for (int j = 0; j < rolesArray.length(); j++) {
                        tmp = rolesArray.getJSONObject(j);
                        roleList.add(jsonDecoder.decodeResource(tmp.toString(), schema, new Role()));
                        try {
                            ResponseValidateTests.runValidateTests(roleList.get(j), schema,
                                    null, null, method,
                                    responseString, headerString.toString(), responseStatus, subTests);

                        } catch (BadRequestException | CharonException e) {
                            long stopTime = System.currentTimeMillis();
                            testResults.add(new TestResult(TestResult.ERROR, requestPath.getTestCaseName(),
                                    "Response Validation Error",
                                    ComplianceUtils.getWire(method, responseString, headerString.toString(),
                                            responseStatus, subTests), stopTime - startTime));
                            errorOccur = true;
                            break;
                        }
                    }
                } catch (JSONException e) {
                    throw new ComplianceException(500, "Error in decoding the returned list resource.");
                } catch (BadRequestException | CharonException | InternalErrorException e) {
                    long stopTime = System.currentTimeMillis();
                    testResults.add(new TestResult(TestResult.ERROR, requestPath.getTestCaseName(),
                            "Could not decode the server response",
                            ComplianceUtils.getWire(method, responseString, headerString.toString(), responseStatus,
                                    subTests), stopTime - startTime));
                    continue;
                }
                switch (requestPath.getTestCaseName()) {
                    case "List roles":  // check for all created roles
                        subTests.add(ComplianceConstants.TestConstants.ALL_GROUPS_IN_TEST);
                        ArrayList<String> returnedRoleIDs = new ArrayList<>();
                        for (Role role : roleList) {
                            returnedRoleIDs.add(role.getId());
                        }
                        for (String id : groupIDs) {
                            if (!returnedRoleIDs.contains(id)) {
                                long stopTime = System.currentTimeMillis();
                                testResults.add(new TestResult(TestResult.ERROR, requestPath.getTestCaseName(),
                                        "Response does not contain all the created roles",
                                        ComplianceUtils.getWire(method, responseString, headerString.toString(),
                                                responseStatus, subTests), stopTime - startTime));
                                errorOccur = true;
                                break;
                            }
                        }
                        break;
                    case "Get roles with Filter": {
                        subTests.add(ComplianceConstants.TestConstants.FILTER_CONTENT_TEST);
                        String value = "loginRole1";
                        for (Role role : roleList) {
                            if (!Objects.equals(value, role.getDisplayName())) {
                                long stopTime = System.currentTimeMillis();
                                testResults.add(new TestResult(TestResult.ERROR, requestPath.getTestCaseName(),
                                        "Response does not contain the expected roles",
                                        ComplianceUtils.getWire(method, responseString, headerString.toString(),
                                                responseStatus, subTests), stopTime - startTime));
                            }
                        }
                        break;
                    }
                    case "Get roles with Pagination":
                    case "Get users having negative number as index":
                        subTests.add(ComplianceConstants.TestConstants.FILTER_CONTENT_TEST);
                        subTests.add(ComplianceConstants.TestConstants.PAGINATION_GROUP_TEST);
                        if (roleList.size() != 2) {
                            long stopTime = System.currentTimeMillis();
                            testResults.add(new TestResult(TestResult.ERROR, requestPath.getTestCaseName(),
                                    "Response does not contain right number of paginated roles",
                                    ComplianceUtils.getWire(method, responseString, headerString.toString(),
                                            responseStatus, subTests), stopTime - startTime));
                            continue;
                        }
                        break;
                    case "Sort test":
                        subTests.add(ComplianceConstants.TestConstants.SORT_GROUPS_TEST);
                        try {
                            if (isRoleListSorted(roleList)) {
                                long stopTime = System.currentTimeMillis();
                                testResults.add(new TestResult(TestResult.ERROR, requestPath.getTestCaseName(),
                                        "Response does not contain the sorted list of roles",
                                        ComplianceUtils.getWire(method, responseString, headerString.toString(),
                                                responseStatus, subTests), stopTime - startTime));
                                continue;
                            }
                        } catch (CharonException e) {
                            long stopTime = System.currentTimeMillis();
                            testResults.add(new TestResult(TestResult.ERROR, requestPath.getTestCaseName(),
                                    "Response does not contain the sorted list of roles",
                                    ComplianceUtils.getWire(method, responseString, headerString.toString(),
                                            responseStatus, subTests), stopTime - startTime));
                            continue;
                        }

                        break;
                    case "Filter with pagination test": {
                        subTests.add(ComplianceConstants.TestConstants.FILTER_USER_WITH_PAGINATION);
                        if (roleList.size() != 1) {
                            long stopTime = System.currentTimeMillis();
                            testResults.add(new TestResult(TestResult.ERROR, requestPath.getTestCaseName(),
                                    "Response does not contain right number of users.",
                                    ComplianceUtils.getWire(method, responseString, headerString.toString(),
                                            responseStatus, subTests), stopTime - startTime));
                            continue;
                        }
                        String value = "loginRole1";
                        for (Role role : roleList) {
                            if (!Objects.equals(value, role.getDisplayName())) {
                                long stopTime = System.currentTimeMillis();
                                testResults.add(new TestResult(TestResult.ERROR, requestPath.getTestCaseName(),
                                        "Response does not contain the expected roles",
                                        ComplianceUtils.getWire(method, responseString, headerString.toString(),
                                                responseStatus, subTests), stopTime - startTime));
                            }
                        }
                        break;
                    }
                    case "Get roles without index and only using count":
                        subTests.add(ComplianceConstants.TestConstants.PAGINATION_USER_TEST);
                        if (startIndex != 1 && count != 2) {
                            long stopTime = System.currentTimeMillis();
                            testResults.add(new TestResult(TestResult.ERROR, requestPath.getTestCaseName(),
                                    "Response does not contain right number of pagination.",
                                    ComplianceUtils.getWire(method, responseString, headerString.toString(),
                                            responseStatus, subTests), stopTime - startTime));
                            continue;
                        }
                        break;
                }
                if (!errorOccur) {
                    long stopTime = System.currentTimeMillis();
                    testResults.add(new TestResult
                            (TestResult.SUCCESS, requestPath.getTestCaseName(),
                                    StringUtils.EMPTY, ComplianceUtils.getWire(method, responseString,
                                    headerString.toString(), responseStatus, subTests), stopTime - startTime));
                }
            } else if (!requestPath.getTestSupported() ||
                    response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_IMPLEMENTED) {
                long stopTime = System.currentTimeMillis();
                testResults.add(new TestResult
                        (TestResult.SKIPPED, requestPath.getTestCaseName(),
                                "This functionality is not implemented. Hence given status code 501",
                                ComplianceUtils.getWire(method,
                                        responseString, headerString.toString(),
                                        responseStatus, subTests), stopTime - startTime));
            } else {
                long stopTime = System.currentTimeMillis();
                testResults.add(new TestResult
                        (TestResult.ERROR, requestPath.getTestCaseName(),
                                StringUtils.EMPTY, ComplianceUtils.getWire(method, responseString,
                                headerString.toString(), responseStatus, subTests), stopTime - startTime));
            }
        }
        // Clean up users after all tasks.
        for (String id : userIDs) {
            cleanUp(id, "User");
        }
        // Clean up groups.
        for (String id : groupIDs) {
            cleanUp(id, "Group");
        }
        // Clean up roles.
        for (String id : roleIDs) {
            cleanUp(id, "Role");
        }
        return testResults;
    }

    /**
     * Get role by id tests.
     *
     * @return testResults Array containing test results.
     * @throws GeneralComplianceException General exceptions.
     * @throws ComplianceException        Constructed new exception with the specified detail message.
     */
    @Override
    public ArrayList<TestResult> getByIdMethodTest() throws GeneralComplianceException, ComplianceException {

        ArrayList<TestResult> testResults;
        testResults = new ArrayList<>();

        ArrayList<String> userIDs;
        ArrayList<String> groupIds;

        RequestPath[] requestPaths;

        RequestPath requestPath1 = new RequestPath();
        requestPath1.setUrl(StringUtils.EMPTY);
        requestPath1.setTestCaseName("Get role by ID");

        RequestPath requestPath2 = new RequestPath();
        requestPath2.setUrl("?attributes=displayName,members.value");
        requestPath2.setTestCaseName("Get a role with specific attributes");

        RequestPath requestPath3 = new RequestPath();
        requestPath3.setUrl("?excludedAttributes=members");
        requestPath3.setTestCaseName("Get a role with excluding attributes");

        RequestPath requestPath4 = new RequestPath();
        requestPath4.setUrl(generateUniqueID());
        requestPath4.setTestCaseName("Role not found error response");

        requestPaths = new RequestPath[]{requestPath1, requestPath2, requestPath3, requestPath4};

        for (RequestPath requestPath : requestPaths) {
            long startTime = System.currentTimeMillis();
            userIDs = createTestsUsers("Many");
            groupIds = createTestsGroups(userIDs, "Many");
            ArrayList<String> roleId = createTestsRoles(userIDs, groupIds, "One");
            String id = roleId.get(0);
            Role role;
            String getRoleURL;
            getRoleURL = url + "/" + id + requestPath.getUrl();
            HttpGet method = new HttpGet(getRoleURL);
            HttpClient client = HTTPClient.getHttpClient();
            HTTPClient.setAuthorizationHeader(complianceTestMetaDataHolder, method);
            method.setHeader(ComplianceConstants.RequestCodeConstants.ACCEPT,
                    ComplianceConstants.RequestCodeConstants.APPLICATION_JSON);
            HttpResponse response = null;
            String responseString = StringUtils.EMPTY;
            StringBuilder headerString = new StringBuilder(StringUtils.EMPTY);
            String responseStatus;
            ArrayList<String> subTests = new ArrayList<>();
            try {
                response = client.execute(method);
                // Read the response body.
                responseString = new BasicResponseHandler().handleResponse(response);
                // Get all headers.
                Header[] headers = response.getAllHeaders();
                for (Header header : headers) {
                    headerString.append(String.format("%s : %s \n", header.getName(), header.getValue()));
                }
                responseStatus = response.getStatusLine().getStatusCode() + " "
                        + response.getStatusLine().getReasonPhrase();
            } catch (Exception e) {
                // Get all headers.
                assert response != null;
                Header[] headers = response.getAllHeaders();
                for (Header header : headers) {
                    headerString.append(String.format("%s : %s \n", header.getName(), header.getValue()));
                }
                responseStatus = response.getStatusLine().getStatusCode() + " "
                        + response.getStatusLine().getReasonPhrase();
                // Clean up users.
                for (String uId : userIDs) {
                    cleanUp(uId, requestPath.getTestCaseName());
                }
                // Clean up groups.
                for (String uId : groupIds) {
                    cleanUp(uId, requestPath.getTestCaseName());
                }
                cleanUp(id, requestPath.getTestCaseName());
                if (!requestPath.getTestCaseName().equals("Group not found error response")) {
                    long stopTime = System.currentTimeMillis();
                    testResults.add(new TestResult(TestResult.ERROR, requestPath.getTestCaseName(),
                            "Could not get the default group from url " + url,
                            ComplianceUtils.getWire(method, responseString, headerString.toString(), responseStatus,
                                    subTests), stopTime - startTime));
                    continue;
                }
            }
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                // Obtain the schema corresponding to group.
                SCIMResourceTypeSchema schema = SCIMResourceSchemaManager.getInstance().getGroupResourceSchema();
                JSONDecoder jsonDecoder = new JSONDecoder();
                try {
                    role = jsonDecoder.decodeResource(responseString, schema, new Role());

                } catch (BadRequestException | CharonException | InternalErrorException e) {
                    // Clean up users.
                    for (String uId : userIDs) {
                        cleanUp(uId, requestPath.getTestCaseName());
                    }
                    // Clean up groups.
                    for (String uId : groupIds) {
                        cleanUp(uId, requestPath.getTestCaseName());
                    }
                    cleanUp(id, requestPath.getTestCaseName());
                    long stopTime = System.currentTimeMillis();
                    testResults.add(new TestResult(TestResult.ERROR, requestPath.getTestCaseName(),
                            "Could not decode the server response",
                            ComplianceUtils.getWire(method, responseString, headerString.toString(), responseStatus,
                                    subTests), stopTime - startTime));
                    continue;
                }
                try {
                    ResponseValidateTests.runValidateTests(role, schema, null,
                            null, method,
                            responseString, headerString.toString(), responseStatus, subTests);
                } catch (BadRequestException | CharonException e) {
                    // Clean up users.
                    for (String uId : userIDs) {
                        cleanUp(uId, requestPath.getTestCaseName());
                    }
                    // Clean up groups.
                    for (String uId : groupIds) {
                        cleanUp(uId, requestPath.getTestCaseName());
                    }
                    cleanUp(id, requestPath.getTestCaseName());
                    long stopTime = System.currentTimeMillis();
                    testResults.add(new TestResult(TestResult.ERROR, requestPath.getTestCaseName(),
                            "Response Validation Error",
                            ComplianceUtils.getWire(method, responseString, headerString.toString(), responseStatus,
                                    subTests), stopTime - startTime));
                    continue;
                }
                // Clean up users.
                for (String uId : userIDs) {
                    cleanUp(uId, requestPath.getTestCaseName());
                }
                // Clean up groups.
                for (String uId : groupIds) {
                    cleanUp(uId, requestPath.getTestCaseName());
                }
                cleanUp(id, requestPath.getTestCaseName());
                long stopTime = System.currentTimeMillis();
                testResults.add(new TestResult
                        (TestResult.SUCCESS, requestPath.getTestCaseName(),
                                StringUtils.EMPTY, ComplianceUtils.getWire(method, responseString,
                                headerString.toString(), responseStatus, subTests), stopTime - startTime));
            } else if (requestPath.getTestCaseName().equals("Role not found error response") &&
                    response.getStatusLine().getStatusCode() == 404) {
                long stopTime = System.currentTimeMillis();
                testResults.add(new TestResult
                        (TestResult.SUCCESS, requestPath.getTestCaseName(),
                                "Server successfully given the expected error 404(Role not found in the user store) " +
                                        "message",
                                ComplianceUtils.getWire(method, responseString, headerString.toString(),
                                        responseStatus, subTests), stopTime - startTime));
            } else {
                // Clean up users.
                for (String uId : userIDs) {
                    cleanUp(uId, requestPath.getTestCaseName());
                }
                cleanUp(id, requestPath.getTestCaseName());
                long stopTime = System.currentTimeMillis();
                testResults.add(new TestResult
                        (TestResult.ERROR, requestPath.getTestCaseName(), StringUtils.EMPTY,
                                ComplianceUtils.getWire(method, responseString, headerString.toString(),
                                        responseStatus, subTests), stopTime - startTime));
            }
        }
        return testResults;
    }

    /**
     * Post role tests.
     *
     * @return testResults Array containing test results.
     * @throws GeneralComplianceException General exceptions.
     * @throws ComplianceException        Constructed new exception with the specified detail message.
     */
    @Override
    public ArrayList<TestResult> postMethodTest() throws GeneralComplianceException, ComplianceException {

        return null;
    }

    /**
     * Patch role tests.
     *
     * @return testResults Array containing test results.
     * @throws GeneralComplianceException General exceptions.
     * @throws ComplianceException        Constructed new exception with the specified detail message.
     */
    @Override
    public ArrayList<TestResult> patchMethodTest() throws GeneralComplianceException, ComplianceException {

        return null;
    }

    /**
     * Put role tests.
     *
     * @return testResults Array containing test results.
     * @throws GeneralComplianceException General exceptions.
     * @throws ComplianceException        Constructed new exception with the specified detail message.
     */
    @Override
    public ArrayList<TestResult> putMethodTest() throws GeneralComplianceException, ComplianceException {

        return null;
    }

    /**
     * Delete role tests.
     *
     * @return testResults Array containing test results.
     * @throws GeneralComplianceException General exceptions.
     * @throws ComplianceException        Constructed new exception with the specified detail message.
     */
    @Override
    public ArrayList<TestResult> deleteMethodTest() throws GeneralComplianceException, ComplianceException {

        return null;
    }

    /**
     * Search role tests.
     *
     * @return testResults Array containing test results.
     * @throws GeneralComplianceException General exceptions.
     * @throws ComplianceException        Constructed new exception with the specified detail message.
     */
    @Override
    public ArrayList<TestResult> searchMethodTest() throws GeneralComplianceException, ComplianceException {

        return null;
    }

    /**
     * Execute all tests.
     *
     * @return null.
     * @throws GeneralComplianceException General exceptions.
     * @throws ComplianceException        Constructed new exception with the specified detail message.
     */
    @Override
    public ArrayList<TestResult> executeAllTests() throws GeneralComplianceException, ComplianceException {

        // This method is not needed for the current implementation.
        return null;
    }
}
