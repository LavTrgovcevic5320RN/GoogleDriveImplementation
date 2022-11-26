package org.example;

import storage.FileMetaData;

public class FileNode {
    private final String ID;
    private FileNodeComposite parent;
    FileMetaData metaData;

    protected void setParent(FileNodeComposite parent) {
        this.parent = parent;
    }

    @Override
    public String toString() {
        return "FileNode{" +
                "ID='" + ID + '\'' +
                ", parent='" + parent + '\'' +
                ", metaData=" + metaData +
                '}';
    }

    public String getID() {
        return ID;
    }

    public FileNodeComposite getParent() {
        return parent;
    }

    public FileNode(String ID, FileMetaData metaData) {
        this.ID = ID;
        this.metaData = metaData;
    }

    public FileNode(String ID) {
        this.ID = ID;
    }
}
