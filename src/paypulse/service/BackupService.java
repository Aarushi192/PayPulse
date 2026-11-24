package paypulse.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;

/** Person B, file I/O: byte-stream file copy and a folder of timestamped backups. */
public class BackupService {

    private final File backupFolder;

    public BackupService(File backupFolder) {
        this.backupFolder = backupFolder;
    }

    /** Copies any file byte by byte using a buffer. Returns the number of bytes copied. */
    public long copy(File from, File to) throws IOException {
        if (!from.isFile()) {
            throw new IOException("Not a file: " + from);
        }
        long total = 0;
        try (InputStream in = new FileInputStream(from);
             OutputStream out = new FileOutputStream(to)) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                total += read;
            }
        }
        return total;
    }

    /** Copies the file into the backup folder as name_20261124-181500.ext and returns the new file. */
    public File backup(File file) throws IOException {
        if (!backupFolder.exists() && !backupFolder.mkdirs()) {
            throw new IOException("Cannot create folder " + backupFolder);
        }
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"));
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        File target = new File(backupFolder, base + "_" + stamp + ext);
        for (int n = 2; target.exists(); n++) {                       // never overwrite an older backup
            target = new File(backupFolder, base + "_" + stamp + "-" + n + ext);
        }
        copy(file, target);
        return target;
    }

    /** All backup files, newest first. */
    public File[] listBackups() {
        File[] files = backupFolder.listFiles(File::isFile);          // lambda-style FileFilter
        if (files == null) {
            return new File[0];                                       // folder does not exist yet
        }
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed()
                .thenComparing(File::getName, Comparator.reverseOrder()));
        return files;
    }

    /** Keeps only the newest 'keep' backups and deletes the rest. Returns how many were deleted. */
    public int deleteOldBackups(int keep) {
        File[] files = listBackups();
        int deleted = 0;
        for (int i = keep; i < files.length; i++) {
            if (files[i].delete()) {
                deleted++;
            }
        }
        return deleted;
    }
}