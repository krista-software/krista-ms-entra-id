package app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util;

import com.microsoft.graph.models.DriveItem;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.ArrayList;

public class FileHierarchyBuilder {
    private FileHierarchyBuilder() {
    }

    public static List<DriveItem> buildHierarchy(List<DriveItem> driveItems) {
        if(driveItems.isEmpty()) return Collections.emptyList();

        Map<String, FileNode> map = new HashMap<>();
        FileNode root = null;

        for (DriveItem item : driveItems) {
            FileNode node = new FileNode(item.id, item);
            map.put(item.id, node);
            if (root == null) {
                root = node;
            }
        }

        driveItems.stream()
                .skip(1)
                .forEach(item -> {
                    if (item.parentReference != null && item.parentReference.id != null) {
                        FileNode parent = map.get(item.parentReference.id);
                        FileNode current = map.get(item.id);
                        if (parent != null && current != null) {
                            parent.addChild(current);
                        }
                    }
                });

        return findLeafNodes(root);
    }

    public static List<DriveItem> findLeafNodes(FileNode node) {
        List<DriveItem> leafNodes = new ArrayList<>();
        findLeafNodesHelper(node, leafNodes);
        return leafNodes;
    }

    private static void findLeafNodesHelper(FileNode node, List<DriveItem> leafNodes) {
        if (node == null) return;

        if (node.getChildren().isEmpty()) {
            leafNodes.add(node.getDriveItem());
        } else {
            for (FileNode child : node.getChildren()) {
                findLeafNodesHelper(child, leafNodes);
            }
        }
    }

}
