package org.example;

import storage.FileMetaData;

import java.util.ArrayList;
import java.util.List;

public class FileNodeComposite extends FileNode{
    final List<FileNode> children = new ArrayList<>();

    public FileNodeComposite(String ID, FileMetaData metaData) {
        super(ID, metaData);
    }

    public FileNodeComposite(String ID) {
        super(ID);
    }

    public List<FileNode> getChildren() {
        return children;
    }

    public boolean add(FileNode fileNode) {
        fileNode.setParent(this);
        return children.add(fileNode);
    }

    public boolean remove(Object o) {
        if(o instanceof FileNode) ((FileNode) o).setParent(null);
        return children.remove(o);
    }
}
