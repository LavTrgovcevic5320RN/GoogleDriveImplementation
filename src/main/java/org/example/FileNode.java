package org.example;

import storage.FileMetaData;

public class FileNode {
    private final String ID;
    private String parent;
    FileMetaData metaData;

    protected void setParent(String parent) {
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

    public String getParent() {
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
