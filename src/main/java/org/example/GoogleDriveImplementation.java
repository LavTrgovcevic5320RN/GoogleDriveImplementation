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

import exceptions.InvalidConstraintException;
import storage.*;

public class GoogleDriveImplementation extends Storage{
    private final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private final String TOKENS_DIRECTORY_PATH = "tokens";
    private final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);
    private final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private Drive driveService;
    private HttpTransport HTTP_TRANSPORT;
    private String root;

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

        //napraviti check if exists metodu i proveriti ovde

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
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void openDirectory(String s) {

    }

    @Override
    public void create(String s, String s1) {

    }

    @Override
    public void create(String s, String s1, int i) {

    }

    @Override
    public void setMaxFiles(String s, int i) {

    }

    @Override
    public void createExpanded(String s, String s1) {

    }

    @Override
    public void uploadFiles(String destination, String... files) throws InvalidConstraintException {
        for(String file: files){
            File fileMetadata = new File();
            java.io.File filePath = new java.io.File(file);
            fileMetadata.setName(filePath.getName());
            fileMetadata.setMimeType("application/vnd.google-apps.unknown");

            FileContent mediaContent = new FileContent("application/pdf", filePath);

            try {
                File realFile = driveService.files().create(fileMetadata, mediaContent)
                        .setFields("id")
                        .execute();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void delete(String name) {
        String fileId = findIDbyFileName(name);
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
        for(String fileName : files){
            try {
                String fileId = findIDbyFileName(fileName);
                driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream);
                outputStream.writeTo(new FileOutputStream(destination + "/" + fileName));
                outputStream.close();
                System.out.println("File " + fileName + " successfully downloaded");
            } catch (IOException e) {
                System.out.println("File " + fileName + " didnt download");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void moveFiles(String s, String... strings) throws InvalidConstraintException {

    }

    @Override
    public void rename(String newName, String oldName) {
        String fileId = findIDbyFileName(oldName);
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
        return null;
    }

    @Override
    public Collection<FileMetaData> searchFilesInAllDirectories(String s) {
        return null;
    }

    @Override
    public Collection<FileMetaData> searchFilesInDirectoryAndBelow(String s) {
        return null;
    }

    @Override
    public Collection<FileMetaData> searchFilesWithExtension(String s, String s1) {
        return null;
    }

    @Override
    public Collection<FileMetaData> searchFilesThatContain(String s, String s1) {
        return null;
    }

    @Override
    public boolean searchIfFilesExist(String s, String... strings) {
        return false;
    }

    @Override
    public Collection<String> searchFile(String s) {
        return null;
    }

    @Override
    public Date getCreationDate(String s) {
        return null;
    }

    @Override
    public Date getModificationDate(String s) {
        return null;
    }

    @Override
    public Collection<FileMetaData> searchByNameSorted(String s, Boolean aBoolean) {
        return null;
    }

    @Override
    public Collection<FileMetaData> searchByDirectoryDateRange(Date date, Date date1, DateType dateType, String s) {
        return null;
    }

    @Override
    public long getStorageByteSize() {
        return 0;
    }

    @Override
    public void setSizeQuota(long l) {

    }

}
