package com.easyms.security.azuread.ms.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.aad.msal4j.*;
import com.microsoft.azure.spring.autoconfigure.aad.*;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.naming.ServiceUnavailableException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
public class CustomAzureADGraphClient {
    private static final SimpleGrantedAuthority DEFAULT_AUTHORITY = new SimpleGrantedAuthority("ROLE_USER");
    private static final String DEFAULT_ROLE_PREFIX = "ROLE_";
    private static final String MICROSOFT_GRAPH_SCOPE = "https://graph.microsoft.com/user.read";
    private static final String AAD_GRAPH_API_SCOPE = "https://graph.windows.net/user.read";

    private final String clientId;
    private final String clientSecret;
    private final ServiceEndpoints serviceEndpoints;
    private final AADAuthenticationProperties aadAuthenticationProperties;

    private static final String V2_VERSION_ENV_FLAG = "v2-graph";
    private boolean aadMicrosoftGraphApiBool;

    public CustomAzureADGraphClient(String clientId, String clientSecret, AADAuthenticationProperties aadAuthProps,
                                    ServiceEndpointsProperties serviceEndpointsProps) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.aadAuthenticationProperties = aadAuthProps;
        this.serviceEndpoints = serviceEndpointsProps.getServiceEndpoints(aadAuthProps.getEnvironment());

        this.initAADMicrosoftGraphApiBool(aadAuthProps.getEnvironment());
    }

    private void initAADMicrosoftGraphApiBool(String endpointEnv) {
        this.aadMicrosoftGraphApiBool = endpointEnv.contains(V2_VERSION_ENV_FLAG);
    }

    private String getUserMembershipsV1(String accessToken) throws IOException {
        final URL url = new URL(serviceEndpoints.getAadMembershipRestUri());
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        // Set the appropriate header fields in the request header.

        if (this.aadMicrosoftGraphApiBool) {
            conn.setRequestMethod(HttpMethod.GET.toString());
            conn.setRequestProperty(HttpHeaders.AUTHORIZATION,
                    String.format("Bearer %s", accessToken));
            conn.setRequestProperty(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        } else {
            conn.setRequestMethod(HttpMethod.GET.toString());
            conn.setRequestProperty("api-version", "1.6");
            conn.setRequestProperty(HttpHeaders.AUTHORIZATION, accessToken);
            conn.setRequestProperty(HttpHeaders.ACCEPT, "application/json;odata=minimalmetadata");
        }
        final String responseInJson = getResponseStringFromConn(conn);
        final int responseCode = conn.getResponseCode();
        if (responseCode == HTTPResponse.SC_OK) {
            return responseInJson;
        } else {
            throw new IllegalStateException("Response is not " + HTTPResponse.SC_OK +
                    ", response json: " + responseInJson);
        }
    }

    private static String getResponseStringFromConn(HttpURLConnection conn) throws IOException {

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            final StringBuilder stringBuffer = new StringBuilder();
            String line = "";
            while ((line = reader.readLine()) != null) {
                stringBuffer.append(line);
            }
            return stringBuffer.toString();
        }
    }

    public List<UserGroup> getGroups(String graphApiToken) throws IOException {
        return loadUserGroups(graphApiToken);
    }

    private List<UserGroup> loadUserGroups(String graphApiToken) throws IOException {
        final String responseInJson = getUserMembershipsV1(graphApiToken);
        final List<UserGroup> lUserGroups = new ArrayList<>();
        final ObjectMapper objectMapper = JacksonObjectMapperFactory.getInstance();
        final JsonNode rootNode = objectMapper.readValue(responseInJson, JsonNode.class);
        final JsonNode valuesNode = rootNode.get("value");

        if (valuesNode != null) {
            lUserGroups
                    .addAll(StreamSupport.stream(valuesNode.spliterator(), false).filter(this::isMatchingUserGroupKey)
                            .map(node -> {
                                final String objectID = node.
                                        get(aadAuthenticationProperties.getUserGroup().getObjectIDKey()).asText();
                                final String displayName = node.get("displayName").asText();
                                return new UserGroup(objectID, displayName);
                            }).collect(Collectors.toList()));

        }

        return lUserGroups;
    }

    /**
     * Checks that the JSON Node is a valid User Group to extract User Groups from
     *
     * @param node - json node to look for a key/value to equate against the
     * {@link AADAuthenticationProperties.UserGroupProperties}
     * @return true if the json node contains the correct key, and expected value to identify a user group.
     */
    private boolean isMatchingUserGroupKey(final JsonNode node) {
        return node.get(aadAuthenticationProperties.getUserGroup().getKey()).asText()
                .equals(aadAuthenticationProperties.getUserGroup().getValue());
    }

    public Set<GrantedAuthority> getGrantedAuthorities(String graphApiToken) throws IOException {
        // Fetch the authority information from the protected resource using accessToken
        final List<UserGroup> groups = getGroups(graphApiToken);

        // Map the authority information to one or more GrantedAuthority's and add it to mappedAuthorities
        return convertGroupsToGrantedAuthorities(groups);
    }


    /**
     * Converts UserGroup list to Set of GrantedAuthorities
     *
     * @param groups
     * @return
     */
    public Set<GrantedAuthority> convertGroupsToGrantedAuthorities(final List<UserGroup> groups) {
        // Map the authority information to one or more GrantedAuthority's and add it to mappedAuthorities
        final Set<GrantedAuthority> mappedAuthorities = groups.stream().filter(this::isValidUserGroupToGrantAuthority)
                .map(userGroup -> new SimpleGrantedAuthority(DEFAULT_ROLE_PREFIX + userGroup.getDisplayName()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (mappedAuthorities.isEmpty()) {
            mappedAuthorities.add(DEFAULT_AUTHORITY);
        }

        return mappedAuthorities;
    }

    /**
     * Determines if this is a valid {@link UserGroup} to build to a GrantedAuthority.
     * <p>
     * If the {@link AADAuthenticationProperties.UserGroupProperties#getAllowedGroups()} or the {@link
     * AADAuthenticationProperties#getActiveDirectoryGroups()} contains the {@link UserGroup#getDisplayName()} return
     * true.
     *
     * @param group - User Group to check if valid to grant an authority to.
     * @return true if either of the allowed-groups or active-directory-groups contains the UserGroup display name
     */
    private boolean isValidUserGroupToGrantAuthority(final UserGroup group) {
        return aadAuthenticationProperties.getUserGroup().getAllowedGroups().contains(group.getDisplayName())
                || aadAuthenticationProperties.getActiveDirectoryGroups().contains(group.getDisplayName());
    }

    public IAuthenticationResult acquireTokenForGraphApi(String idToken, String tenantId)
            throws ServiceUnavailableException {
        final IClientCredential clientCredential = ClientCredentialFactory.create(clientSecret);
        final UserAssertion assertion = new UserAssertion(idToken);

        IAuthenticationResult result = null;
        ExecutorService service = null;
        try {
            service = Executors.newFixedThreadPool(1);

//            final ConfidentialClientApplication application = ConfidentialClientApplication.builder(clientId,
//                    clientCredential).build();

            final ConfidentialClientApplication application = ConfidentialClientApplication.builder(clientId,
                    clientCredential).authority("https://login.microsoftonline.com/" + tenantId + "/").build();

            final Set<String> scopes = new HashSet<>();
            scopes.add(aadMicrosoftGraphApiBool ? MICROSOFT_GRAPH_SCOPE : AAD_GRAPH_API_SCOPE);

            final OnBehalfOfParameters onBehalfOfParameters = OnBehalfOfParameters
                    .builder(scopes, assertion)
                    .build();

            final CompletableFuture<IAuthenticationResult> future = application.acquireToken(onBehalfOfParameters);
            result = future.get();
        } catch (Exception e) {
            // handle conditional access policy
            final Throwable cause = e.getCause();
            if (cause instanceof MsalServiceException) {
                final MsalServiceException exception = (MsalServiceException) cause;
                if (exception.claims() != null && !exception.claims().isEmpty()) {
                    throw exception;
                }
            }
            log.error("acquire on behalf of token for graph api error", e);
        } finally {
            if (service != null) {
                service.shutdown();
            }
        }

        if (result == null) {
            throw new ServiceUnavailableException("unable to acquire on-behalf-of token for client " + clientId);
        }
        return result;
    }
}
