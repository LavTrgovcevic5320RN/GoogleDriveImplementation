package org.example;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.*;

import exceptions.InvalidConstraintException;
import storage.*;

public class GoogleDriveImplementation extends Storage{
    /**
     * Application name.
     */
    private static final String APPLICATION_NAME = "Google Drive Implementation";
    /**
     * Global instance of the JSON factory.
     */
    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    /**
     * Directory to store authorization tokens for this application.
     */
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    /** Creates an authorized Credential object.
     *
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = GoogleDriveImplementation.class.getResourceAsStream("/client_secret.json");
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + "/client_secret.json");
        }
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        //returns an authorized Credential object.
        return credential;
    }

//    public static void main(String... args) throws IOException, GeneralSecurityException {
//        // Build a new authorized API client service.
//        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
//        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
//                .setApplicationName(APPLICATION_NAME)
//                .build();
//
//        // Print the names and IDs for up to 10 files.
//        FileList result = service.files().list()
//                .setPageSize(10)
//                .setFields("nextPageToken, files(id, name)")
//                .execute();
//        List<File> files = result.getFiles();
//        if (files == null || files.isEmpty()) {
//            System.out.println("No files found.");
//        } else {
//            System.out.println("Files:");
//            for (File file : files) {
//                System.out.printf("%s (%s)\n", file.getName(), file.getId());
//            }
//        }
//        GoogleDriveImplementation g = new GoogleDriveImplementation();
//        g.initialiseDirectory("SK", "C:\\Users\\Lav\\Desktop\\Adasdasd", 2048, 5,  "exe");
//
//    }

    private static HttpTransport HTTP_TRANSPORT;
    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    public static Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in = GoogleDriveImplementation.class.getResourceAsStream("/client_secret.json");
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
                clientSecrets, SCOPES).setAccessType("offline").build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
        return credential;
    }

    public static Drive getDriveService() throws IOException {
        Credential credential = authorize();
        return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static void main(String[] args) throws IOException {
        GoogleDriveImplementation g = new GoogleDriveImplementation();
//        g.initialiseDirectory("/my-drive", "marko polo", 256, 5, "exe");
//        g.delete("B"); // primer za folder unutar folder-a A npr. A/B
//        g.delete("1"); // primer za file
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
            file =  GoogleDriveImplementation.getDriveService().files().create(file)
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
    public void uploadFiles(String s, String... strings) throws InvalidConstraintException {

    }

    @Override
    public void delete(String name) {
        try {
            FileList result = getDriveService().files().list()
                    .setPageSize(10)
                    .setFields("files(id, name)")
                    .execute();

            List<File> files = result.getFiles();
            if (files == null || files.isEmpty())
                System.out.println("No files found.");
            else
                for (File file : files)
                    if(file.getName().equals(name)) {
                        getDriveService().files().delete(file.getId()).execute();
                        System.out.println("Folder deleted.");
                    }
            System.out.println("Folder doesn't exist 1.");
        } catch (IOException e) {
            System.out.println("Folder doesn't exist 2.");
        }
    }

    @Override
    public void moveFiles(String s, String... strings) throws InvalidConstraintException, FileNotFoundException {

    }

    @Override
    public void download(String s, String... strings) {

    }

    @Override
    public void rename(String s, String s1) {

    }

    @Override
    public long getStorageByteSize() {
        return 0;
    }

    @Override
    public void setSizeQuota(long l) {

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

}
