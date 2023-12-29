package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.*;
import okhttp3.Request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.List;

public class FileCreateService {

    private static final Logger log = LoggerFactory.getLogger(FileCreateService.class);

    private final GraphServiceClient<Request> graphClient;

    public FileCreateService(GraphServiceClient<Request> graphClient) {
        this.graphClient = graphClient;
    }

    /**
     * Creates a new group in Microsoft Graph.
     *
     * @param map A map containing properties of the group.
     *            - "Name": The display name of the group.
     *            - "Base Name": The mail nickname or base name of the group.
     *            (Additional properties can be included based on requirements.)
     * @return A {@link GroupImpl} representing the created group.
     * If the group is successfully created, the returned object contains the group details.
     * If there is an error during the creation process, the returned object contains the error details.
     * @throws IllegalArgumentException If the input map is null or missing required properties.
     * @see GroupImpl
     */
    public GroupImpl createGroup(Map<String, Object> groupData) {
        try {
            // Validate input map
            if (groupData == null) {
                log.error("Invalid input: groupData map is null");
                return new GroupImpl(null, null);
            }

            // Extract group properties with null checks
            String groupName = (String) groupData.get("Name");
            String groupBaseName = (String) groupData.get("Base Name");
            if (groupName == null || groupBaseName == null) {
                log.error("Invalid input: Missing group name or base name");
                return new GroupImpl(null, null);
            }

            // Create a new group object
            Group groupToCreate = new Group();
            groupToCreate.displayName = groupName;
            groupToCreate.mailNickname = groupBaseName;
            groupToCreate.groupTypes = Collections.singletonList("Unified");
            groupToCreate.mailEnabled = true;
            groupToCreate.securityEnabled = false;

            // Send the request to create the group
            Group createdGroup = graphClient.groups().buildRequest().post(groupToCreate);

            return new GroupImpl(createdGroup, null);
        } catch (GraphServiceException e) {
            log.error("Error creating group", e);
            return new GroupImpl(null, e);
        }
    }
}