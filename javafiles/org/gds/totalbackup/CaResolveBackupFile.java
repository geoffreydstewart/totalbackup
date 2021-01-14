package org.gds.totalbackup;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves a single backup file from a specified source directory
 * <p>
 * Sets the property: the full path to the backup file
 * Expects the property: the full path to the source directory which contains the backup file
 * <p>
 * org.gds.totalbackup.CaResolveBackupFile
 */
public class CaResolveBackupFile extends Task {
    private File _backupFileSourcePath;

    public void setBackupFileSourcePath(File backupFileSourcePath) {
        _backupFileSourcePath = backupFileSourcePath;
    }

    public void execute() {
        assertBackupFileSourcePath();

        List<Path> matchedFiles = new ArrayList<>();
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(_backupFileSourcePath.toPath(), "*.zip")) {
            dirStream.forEach(matchedFiles::add);
        } catch (IOException e) {
            log("No backup file was resolved at the path " + _backupFileSourcePath.getPath());
            getProject().setProperty("resolvedBackupFilePath", "nofile");
        }

        if (matchedFiles.size() != 1) {
            log("The number of files found at " + _backupFileSourcePath.getPath() + " was not 1.  There was "
                    + matchedFiles.size());
            getProject().setProperty("resolvedBackupFilePath", "nofile");
        } else {
            String backupFilePathString = matchedFiles.get(0).toString();
            log("The resolved backup file path is " + backupFilePathString);
            getProject().setProperty("resolvedBackupFilePath", backupFilePathString);
        }
    }

    private void assertBackupFileSourcePath() throws BuildException {
        if (null == _backupFileSourcePath)
            throw new BuildException("The backup file source path must exist");
    }

}

