package app.krista.extensions.essentials.cloudfiles.sharepoint.dao.util;

import com.microsoft.graph.models.DriveItem;

import java.util.ArrayList;
import java.util.List;

public class FileNode {
    private final String id;
    private final List<FileNode> children;

    private final DriveItem driveItem;

    public FileNode(String id, DriveItem driveItem) {
        this.id = id;
        this.children = new ArrayList<>();
        this.driveItem = driveItem;
    }

    public String getId() {
        return id;
    }

    public DriveItem getDriveItem() {
        return driveItem;
    }

    public List<FileNode> getChildren() {
        return children;
    }

    public void addChild(FileNode child) {
        children.add(child);
    }
}
