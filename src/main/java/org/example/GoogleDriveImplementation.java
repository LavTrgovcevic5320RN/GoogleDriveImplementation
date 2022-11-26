package org.example;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import exceptions.FileException;
import exceptions.InvalidConstraintException;
import storage.*;

public class GoogleDriveImplementation extends Storage{
    private final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private final String TOKENS_DIRECTORY_PATH = "tokens";
    private final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);
    private final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private Drive driveService;
    private HttpTransport HTTP_TRANSPORT;
    private FileNodeComposite driveRootNode;
    private FileNodeComposite rootNode;
    private String relativeOffset = "";

    //Potrebni fields iz fajla
    private static final String getFields = "files(id,name,mimeType,trashed,parents,modifiedTime,createdTime,viewedByMeTime,ownedByMe,size),nextPageToken";

    public GoogleDriveImplementation() {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            Credential credential = authorize();
            String APPLICATION_NAME = "Google Drive Implementation";
            driveService = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    public Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in = GoogleDriveImplementation.class.getResourceAsStream("/client_secret.json");
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
                clientSecrets, SCOPES).setAccessType("offline").build();
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    public String findIDbyFileName(String fileName){
        String fileID = null;
        try {
            FileList result = driveService.files().list()
                    .setFields("files(id, name)")
                    .execute();

            List<File> files = result.getFiles();
            if (files == null || files.isEmpty())
                System.out.println("No files found.");
            else
                for (File file : files)
                    if(file.getName().equals(fileName)) {
                        return file.getId();
                    }
            System.out.println("Folder doesn't exist 1.");
        } catch (IOException e) {
            System.out.println("Folder doesn't exist 2.");
        }

        return null;
    }

    public String findRoot() {
        List<File> v = getMyFiles();
        return findRootRec(v, null, null);
    }

    private String findRootRec(List<File> all, String currentHighest, HashSet<String> parents) {
        File r = all.get(all.size() / 2);
        if(currentHighest == null || parents == null) {
            currentHighest = r.getParents().get(0);
            parents = new HashSet<>();
            for(File f : all) parents.addAll(f.getParents());
        }

        boolean changed = false;
        for(File f : all) if(f.getId().equals(currentHighest)) {
            if(f.getParents().get(0) != null) {
                currentHighest = f.getParents().get(0);
                changed = true;
                break;
            }
        }
        if(changed) return findRootRec(all, currentHighest, parents);
        else return currentHighest;
    }

    public List<File> getFilesInFolder(String fid) {
        FileList list;
        List<File> fileList = new ArrayList<>();
        String more = null;
        try {
            do {
                list = ((driveService.files().list().setSpaces("drive").setCorpora("user").set("includeItemsFromAllDrives", false).setPageSize(1000).setQ(String.format("'%s' in parents", fid))
                        .setFields(getFields).setPageToken(more).execute()));
                fileList.addAll(list.getFiles());
                more = list.getNextPageToken();
            } while (more != null);
            return defaultFilter(fileList);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        return null;
    }

    private List<File> defaultFilter(Collection<File> toFilter) {
        return toFilter.stream().filter(File::getOwnedByMe).filter(Predicate.not(File::getTrashed)).sorted(Comparator.comparing(File::getName)).collect(Collectors.toList());
    }

    public List<File> getMyFiles() {
        FileList list;
        List<File> fileList = new ArrayList<>();
        String more = null;
        try {
            do {
                list = ((driveService.files().list().setSpaces("drive").setCorpora("user").set("includeItemsFromAllDrives", false).setPageSize(1000).setPageToken(more)
                        .setFields(getFields).execute()));
                fileList.addAll(list.getFiles());
                more = list.getNextPageToken();
            } while (more != null);
            return defaultFilter(fileList);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        return null;
    }

    /*public List<File> listFolder(String fid) {

    }*/

    public static void main(String[] args) throws IOException {
        GoogleDriveImplementation g = new GoogleDriveImplementation();
        List<File> v = g.getMyFiles();
//        g.printFiles(v);
//        System.out.println();
//        g.printFiles(g.getFilesInFolder(g.findRoot()));
        // System.out.println(g.getMyFiles());
        // g.initialiseDirectory("/my-drive", "marko polo", 256, 5, "exe");
//        g.delete("B"); // primer za folder unutar folder-a npr. A/B
//        g.delete("1"); // primer za file

        //g.download("C:/Users/Lav/Desktop/Adasdasd", "A");
//        g.rename("1.pdf","SK-prvi projekat2022.pdf");
//        g.uploadFiles("Kuca", "C:/Users/Lav/Desktop/1.txt");
//        g.uploadFiles("Kuca", "C:/Users/Lav/Desktop/2.xlsx");
        g.uploadFiles("Kuca", "C:/Users/Lav/Desktop/3.jpg");
    }

    private static final int MIN_IDENT_LEN = 100;
    public void printTree(FileNode root, int ident) {
        String identStr = " ".repeat(ident);
        int innerSpacing = MIN_IDENT_LEN - (identStr.length() + root.metaData.getName().length() + (root instanceof FileNodeComposite ? 1 : 0) + root.getID().length());
        if(innerSpacing <= 0) innerSpacing = 1;
        System.out.printf("%s%s%s%s\n", identStr, root.metaData.getName() + (root instanceof FileNodeComposite ? "/" : ""),  " ".repeat(innerSpacing), root.getID());
        if(root instanceof FileNodeComposite) {
            for(FileNode f : ((FileNodeComposite) root).children) printTree(f, ident+2);
        }
    }

    private void printFiles(List<File> list) {
        for(File f : list) {
            System.out.printf("%s %s %s%n", f.getName(), f.getId(), f.getParents());
        }
    }

    private FileNodeComposite createTree() {
        HashMap<String, FileNode> nodes = new HashMap<>();
        List<File> list = getMyFiles();
        list.forEach(file -> {
            if(file.getMimeType().equals("application/vnd.google-apps.folder")) nodes.put(file.getId(), new FileNodeComposite(file.getId(), readFileMetadata(file)));
        });
        String rid = findRoot();
        FileNodeComposite fnc = (FileNodeComposite) nodes.get(rid);
        if(fnc == null) fnc = new FileNodeComposite(rid);
        fnc.metaData = new FileMetaData("#", rid);
        nodes.put(fnc.getID(), fnc);
        list.forEach(file -> {
            if(nodes.containsKey(file.getParents().get(0))) ((FileNodeComposite)nodes.get(file.getParents().get(0)))
                    .add(nodes.containsKey(file.getId()) ? nodes.get(file.getId()) : new FileNode(file.getId(), readFileMetadata(file)));
        });
        return fnc;
    }

    private FileMetaData readFileMetadata(File f) {
        return new FileMetaData(f.getName(), f.getId(), f.getModifiedTime() == null ? null : new Date(f.getModifiedTime().getValue()),
                f.getViewedByMeTime() == null ? null :new Date(f.getViewedByMeTime().getValue()),
                f.getCreatedTime() == null ? null :new Date(f.getCreatedTime().getValue()), f.getSize() == null ? 0 : f.getSize(),
                f.getMimeType().equals("application/vnd.google-apps.folder") ? FileMetaData.Type.DIRECTORY : FileMetaData.Type.FILE);
    }

    @Override
    public void initialiseDirectory(String path, String storageName, int size, int maxFiles, String... bannedExtensions) {
        File file = new File();
        file.setName(storageName);
        file.setMimeType("application/vnd.google-apps.folder");
        driveRootNode = createTree();
        if(absolutePathToID(path + "/" + storageName) != null) throw new FileException("That path already exists!");

        storageConstraint = new StorageConstraint();
        if (size >= 0)
            storageConstraint.setByteSizeQuota(size);
        storageConstraint.getMaxNumberOfFiles().put("#", maxFiles >= 0 ? maxFiles : -1);

        if (bannedExtensions.length > 0) {
            for(int i = 0 ; i < bannedExtensions.length; i++)
                bannedExtensions[i] = bannedExtensions[i].toLowerCase();
            storageConstraint.getIllegalExtensions().addAll(Arrays.asList(bannedExtensions));
        }

        try {
            file =  driveService.files().create(file)
                    .setFields("id,parents")
                    .execute()
                    .setQuotaBytesUsed(Long.parseLong(storageConstraint.getByteSizeQuota() + ""));
            System.out.println("New Root ID: " + file.getId());
            FileNodeComposite newNode = new FileNodeComposite(file.getId(), new FileMetaData(storageName, path + "/" + storageName));
            ((FileNodeComposite)getNode(absolutePathToID(path))).add(newNode);
            rootNode = newNode;
            relativeOffset = path +"/" + storageName;
            writeConfiguration();
//            printTree(driveRootNode, 0);
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    private FileNode getNode(String id) {
        return getNodeRec(id, driveRootNode);
    }

    private FileNode getNodeRec(String target, FileNodeComposite currentDir) {
        if(target.equals(currentDir.getID())) return currentDir;
        for(FileNode f : currentDir.getChildren()) {
            if(f.getID().equals(target)){
                return f;
            }
            else if(f instanceof FileNodeComposite) {
                FileNode r = getNodeRec(target, (FileNodeComposite) f);
                if(r != null) return r;
            }
        }
        return null;
    }

    private String absolutePathToID(String path) {
        if(driveRootNode == null) driveRootNode = createTree();
        FileNode currentNode = driveRootNode;
        path = path.replaceAll("[/\\\\]+", "/");
        path = path.replaceAll("(?<!^)/*$", "");
        if(path.startsWith("/")) {
            String[] split = path.split("/");
            int level = 1;
            if(split.length == 0) return driveRootNode.getID();
            while(level != split.length) {
                if(!(currentNode instanceof FileNodeComposite)) return null;
                else {
                    for (FileNode n : ((FileNodeComposite) currentNode).getChildren()) {
                        if(n.metaData.getName().equals(split[level])) {
                            currentNode = n;
                            if(level + 1 == split.length) return currentNode.getID();
                            break;
                        }
                    }
                    level++;
                }
            }
        }
        return null;
    }

    private String getAbsolutePath(String relativePath) {
        relativePath = relativePath.replaceAll("\\\\", "/");
        relativePath = relativePath.replaceAll("/*$", "");
        return relativePath.replaceAll("#/*", relativeOffset + "/").replaceAll("/+", "/");
    }

    @Override
    public void openDirectory(String s) {
        download(".", s + "/directory.conf");
        s = s.replaceAll("\\\\", "/");
        s = s.replaceAll("/+", "/");
        s = s.replaceAll("/*$", "");
        relativeOffset = s;
        driveRootNode = createTree();
        rootNode = (FileNodeComposite) getNode(absolutePathToID(s));
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new java.io.File("directory.conf"))))  {
            storageConstraint = (StorageConstraint) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        System.out.println(storageConstraint);
    }

    private void writeConfiguration() {
        java.io.File f = new java.io.File("directory.conf");
        FileNode conf = getNode(localPathToID("#/directory.conf"));
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f)))  {
            oos.writeObject(storageConstraint);
            File fileMetadata = new File();
            fileMetadata.setParents(Collections.singletonList(absolutePathToID(getAbsolutePath("#/"))));
            driveService.files().update(conf.getID(), fileMetadata);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setMaxFiles(String s, int i) {

    }

    @Override
    public void uploadFiles(String destination, String... files) throws InvalidConstraintException {
        for(String file: files){
            File fileMetadata = new File();
            java.io.File filePath = new java.io.File(file);
            fileMetadata.setName(filePath.getName());
            fileMetadata.setMimeType("application/octet-stream");
            fileMetadata.setParents(Collections.singletonList(absolutePathToID(getAbsolutePath(destination))));

            FileContent mediaContent = new FileContent("application/octet-stream", filePath);

            try {
                File realFile = driveService.files().create(fileMetadata, mediaContent)
                        .setFields("id, parents")
                        .execute();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void delete(String name) {
        String clearedName = getAbsolutePath(name);
        String fileId = absolutePathToID(clearedName);
        try {
            driveService.files().delete(fileId).execute();
            System.out.println("Folder deleted.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void download(String destination, String... files) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for(String path : files){
            try {
                String fileName = path.substring(Math.max(path.lastIndexOf("/"), path.lastIndexOf("\\"))+1);
                String clearedName = getAbsolutePath(path);
                String fileId = absolutePathToID(clearedName);
                driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream);
                outputStream.writeTo(new FileOutputStream(destination + "/" + fileName));
                outputStream.close();
                System.out.println("File " + path + " successfully downloaded");
            } catch (IOException e) {
                System.out.println("File " + path + " didnt download");
                e.printStackTrace();
            }
        }
    }

    private boolean checkIfAdditionValid(String path, int add) {
        FileNode node = getNode(localPathToID(path));
        if (!(node instanceof FileNodeComposite)) return false;
        int noFiles = ((FileNodeComposite) node).getChildren().size();
        int allowedFiles;
        path = path.replaceFirst("[/\\\\]$", "");
        allowedFiles = storageConstraint.getMaxNumberOfFiles().get(path);
        if(allowedFiles < 0) return true;
        return (allowedFiles >= noFiles + add);
    }

    // returns false if extension illegal. true if legal
    private boolean checkExtension(String file) {
        String ext = file.substring(file.lastIndexOf(".")+1).toLowerCase();
        return (!storageConstraint.getIllegalExtensions().contains(ext));
    }

    public void create(String directoryName, String path) {
        create(directoryName, path, -1);
    }

    private boolean bulkMode = false;

    @Override
    public void create(String directoryName, String path, int i) {
        if(checkIfAdditionValid(path, 1)) {
            if(localPathToID(path + "/" + directoryName) == null || getNode(localPathToID(path + "/" + directoryName)) == null) {
                File file = new File();
                file.setName(directoryName);
                file.setMimeType("application/vnd.google-apps.folder");
                file.setParents(Collections.singletonList(localPathToID(path)));
                try {
                    file =  driveService.files().create(file)
                            .setFields("id,parents")
                            .execute();
                    ((FileNodeComposite)getNode(localPathToID(path))).add(new FileNodeComposite(file.getId(), new FileMetaData(directoryName, path + "/" + directoryName)));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            else System.err.println("Directory exists! " + path + "/" + directoryName);
            storageConstraint.getMaxNumberOfFiles().put(path +  directoryName, i);
            if(!bulkMode) writeConfiguration();
        } else throw new InvalidConstraintException("Directory full");
    }

    @Override
    public void createExpanded(String path, String pattern) {
        /*bulkMode = true;
        if(checkIfAdditionValid(path, BraceExpansion.getTopLevelDirectoryCount(pattern)))
            for(String s : BraceExpansion.expand(pattern)){
                String full = path + s;
                full = full.replaceAll("\\\\+", "/");
                full = full.replaceAll("/+", "/");
                int index = Math.max(full.lastIndexOf("/"), full.lastIndexOf("\\"));
                String actualName = full.substring(index +1);
                String actualPath = full.substring(0, index +1);
                System.out.printf("actn %s actp %s\n", actualName, actualPath);
                create(actualName, actualPath);
            }
        else throw new InvalidConstraintException("Too many files!");
        writeConfiguration();
        bulkMode = false;*/
    }

    @Override
    public void moveFiles(String s, String... strings) throws InvalidConstraintException {

    }

    @Override
    public void rename(String newName, String oldName) {
        String clearedName = getAbsolutePath(oldName);
        String fileId = absolutePathToID(clearedName);
        try {
            File file = driveService.files().get(fileId).execute();

            file.setName(newName);
            File f = new File();
            f.setProperties(file.getProperties());
            f.setName(newName);

            File updatedFile = driveService.files().update(fileId, f).execute();

            System.out.println("New file name : " + updatedFile.getName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Collection<FileMetaData> searchFilesInDirectory(String s) {
        Collection<FileMetaData> ret = new ArrayList<>();
        FileNode folder = getNode(localPathToID(s));
        if(folder instanceof FileNodeComposite) {
            ((FileNodeComposite) folder).children.forEach(fileNode -> ret.add(fileNode.metaData));
        }
        return ret;
    }

    private Collection<FileMetaData> searchRecursive(FileNode node) {
        Collection<FileMetaData> ret = new ArrayList<>();
        if(node instanceof FileNodeComposite) {
            ((FileNodeComposite) node).children.forEach(fileNode -> ret.addAll(searchRecursive(fileNode)));
        }
        ret.add(node.metaData);
        return ret;
    }

    @Override
    public Collection<FileMetaData> searchFilesInDirectoryAndBelow(String s) {
        return searchRecursive(getNode(localPathToID(s)));
    }

    @Override
    public Collection<FileMetaData> searchFilesWithExtension(String path, String extension) {
        extension = extension.trim();
        extension = extension.toLowerCase();
        if(!extension.matches("\\.?[\\w\\d.]+")) throw new RuntimeException(String.format("Extension \"%s\" is not valid", extension));
        Collection<FileMetaData> allFiles = searchFilesInDirectoryAndBelow(path);
        final String finalExtension = extension;
        return allFiles.stream().filter(fileMetaData -> fileMetaData.getName().toLowerCase().endsWith(finalExtension)).collect(Collectors.toList());
    }

    @Override
    public Collection<FileMetaData> searchFilesThatContain(String path, String substring) {
        Collection<FileMetaData> allFiles = searchFilesInDirectoryAndBelow(path);
        final String finalSubstring = substring.toLowerCase();
        return allFiles.stream().filter(fileMetaData -> fileMetaData.getName().toLowerCase().contains(finalSubstring)).collect(Collectors.toList());
    }

    public boolean searchIfFilesExist(String s, String... strings) {
        Collection<FileMetaData> allFiles = searchFilesInDirectory(s);
        Collection<String> names = new HashSet<>();
        for(FileMetaData f : allFiles) names.add(f.getName());
        return names.containsAll(Arrays.asList(strings));
    }

    @Override
    public Collection<String> searchFile(String s) {
        Collection<FileMetaData> allFiles = searchFilesInDirectoryAndBelow("#");
        Collection<FileMetaData> matching = allFiles.stream().filter(fileMetaData -> fileMetaData.getName().equalsIgnoreCase(s)).collect(Collectors.toList());
        Collection<String> ids = new HashSet<>();
        for (FileMetaData f : matching) ids.add(f.getFullPath());
        Collection<String> paths = new ArrayList<>();
        for (String id : ids) paths.add(getPath(getNode(id)));
        return paths;
    }

    private String getPath(FileNode f) {
        StringBuilder ret = new StringBuilder();
        while (!f.equals(rootNode)) {
            ret.insert(0, f.metaData.getName() + "/");
            f = f.getParent();
            if(f == null) break;
        }
        ret.insert(0, "#/");
        ret.deleteCharAt(ret.length()-1);
        return ret.toString();
    }

    @Override
    public Collection<FileMetaData> searchByNameSorted(String path, boolean ascending) {
        Collection<FileMetaData> allFiles = searchFilesInDirectory(path);
        Sorter s = new Sorter(ascending);
        return s.applySorter(allFiles);
    }

    @Override
    public Collection<FileMetaData> searchByDirectoryDateRange(Date startDate, Date endDate, DateType sortDateType, String path) {
        Collection<FileMetaData> allFiles = searchFilesInDirectory(path);
        Filter f = new Filter(startDate, endDate, sortDateType);
        return f.applyFilter(allFiles);
    }

    @Override
    public long getStorageByteSize() {
        return 0;
    }

    @Override
    public void setSizeQuota(long l) {

    }

    private String localPathToID(String path) {
        return absolutePathToID(getAbsolutePath(path));
    }

}
