package app.krista.extensions.essentials.cloudfiles.sharepoint.impl;

import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.*;
import okhttp3.Request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class FileCreateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileCreateService.class);

    private final GraphServiceClient<Request> graphClient;

    public FileCreateService(GraphServiceClient<Request> graphClient) {
        this.graphClient = graphClient;
    }

    /**
     * Creates a new group using the provided groupData map, validates the input, and performs the group creation.
     *
     * @param groupData A map containing properties for group creation, including "Name" and "Base Name".
     * @return A GroupImpl object representing the newly created group, or null if creation fails.
     * @see GroupImpl
     * @see GraphServiceException
     */
    public GroupImpl createGroup(Map<String, Object> groupData) {
        try {
            if (!validateGroupData(groupData)) {
                LOGGER.error("Invalid input: Missing group data or properties");
                throw new IllegalArgumentException("Invalid input: Missing group data or properties");
            }

            Group groupToCreate = buildGroup(groupData);
            Group createdGroup = graphClient.groups().buildRequest().post(groupToCreate);

            return new GroupImpl(createdGroup, null);
        } catch (GraphServiceException cause) {
            LOGGER.error("Error creating group", cause.getMessage());
            throw new IllegalArgumentException("Error : "+cause.getMessage());
        }
    }

    private boolean validateGroupData(Map<String, Object> groupData) {
        String groupName = (String) groupData.get("Name");
        String groupBaseName = (String) groupData.get("Base Name");

        return groupData != null
                && groupName != null && !groupName.trim().isEmpty()
                && groupBaseName != null && !groupBaseName.trim().isEmpty()
                && groupName.matches("[a-zA-Z0-9 ]+")  // Allow only letters, numbers, and spaces
                && groupBaseName.matches("[a-zA-Z0-9 ]+")
                && !groupBaseName.contains(" ");
    }

    private Group buildGroup(Map<String, Object> groupData){
        Group groupToCreate = new Group();
        groupToCreate.displayName = (String) groupData.get("Name");
        groupToCreate.mailNickname = ((String) groupData.get("Base Name")).trim();
        groupToCreate.groupTypes = Collections.singletonList("Unified");
        groupToCreate.mailEnabled = true;
        groupToCreate.securityEnabled = false;
        return groupToCreate;
    }

}