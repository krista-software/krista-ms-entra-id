package app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util;

import com.microsoft.graph.models.DriveItem;

import java.util.*;

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

        for (int i = 1; i < driveItems.size(); i++) {
            DriveItem item = driveItems.get(i);
            if (item.parentReference != null && item.parentReference.id != null) {
                FileNode parent = map.get(item.parentReference.id);
                FileNode current = map.get(item.id);
                if (parent != null && current != null) {
                    parent.addChild(current);
                }
            }
        }

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
