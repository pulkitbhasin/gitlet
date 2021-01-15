package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

/** Commands related to output.
 * @author pulkit  */
public class ViewCommands {

    /** Commit details for current branch. */
    public static void log() throws IOException {
        Commit currentCommit = Branch.getCurrentBranch().getHeadCommit();
        while (currentCommit != null) {
            currentCommit.print();
            currentCommit = currentCommit.getParent();
        }
    }

    /** Commit details for all branches. */
    public static void globalLog() throws IOException {
        for (String fileName : Commands.COMMITS.list()) {
            File file = Utils.join(Commands.COMMITS, fileName);
            Commit commit = Utils.readObject(file, Commit.class);
            file.delete();
            file.createNewFile();
            Utils.writeObject(file, commit);
            commit.print();
        }
    }

    /** Find commit(s) with specific message.
     * @param message  */
    public static void find(String message)
            throws IOException {
        boolean flag = false;
        for (String fileName : Commands.COMMITS.list()) {
            File file = Utils.join(Commands.COMMITS, fileName);
            Commit commit = Utils.readObject(file, Commit.class);
            file.delete();
            file.createNewFile();
            Utils.writeObject(file, commit);
            if (commit.getMessage().equals(message)) {
                System.out.println(commit.getName());
                flag = true;
            }
        }
        if (!flag) {
            Commands.exitWithError("Found no commit with that message.");
        }
    }

    /** Displays current status. */
    public static void status() throws IOException {
        System.out.println("=== Branches ===");
        List<String> branchNames = Utils.plainFilenamesIn(Commands.BRANCHES);
        for (String branchName : branchNames) {
            File file = Utils.join(Commands.BRANCHES, branchName);
            Branch branch = Utils.readObject(file, Branch.class);
            if (Branch.getCurrentBranch().getName().equals(branch.getName())) {
                System.out.println("*" + branch.getName());
            } else {
                System.out.println(branch.getName());
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        List<String> stagedFileNames =
                Utils.plainFilenamesIn(Commands.STAGING_DIRECTORY);
        for (String fileName : stagedFileNames) {
            File stagedFile = Utils.join(Commands.STAGING_DIRECTORY, fileName);
            Blob blob = Utils.readObject(stagedFile, Blob.class);
            if (blob.getAddition()) {
                System.out.println(fileName);
            }
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (String fileName : stagedFileNames) {
            File stagedFile = Utils.join(Commands.STAGING_DIRECTORY, fileName);
            Blob blob = Utils.readObject(stagedFile, Blob.class);
            if (blob.getRemoval()) {
                System.out.println(fileName);
            }
        }
        Commit currentCommit = Branch.getCurrentBranch().getHeadCommit();
        Hashtable<String, Blob> blobs = currentCommit.getBlobs();
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        modificationsNot();
        System.out.println();
        System.out.println("=== Untracked Files ===");
        for (String fileName : Utils.plainFilenamesIn(Commands.CWD)) {
            File file = Utils.join(Commands.STAGING_DIRECTORY, fileName);
            if (!file.exists() && !blobs.containsKey(fileName)) {
                System.out.println(fileName);
            }
        }
        System.out.println();
    }

    /** Modifications not staged for commit status helper. */
    public static void modificationsNot() throws IOException {
        Commit currentCommit = Branch.getCurrentBranch().getHeadCommit();
        Hashtable<String, Blob> blobs = currentCommit.getBlobs();
        for (String fileName : Utils.plainFilenamesIn(Commands.CWD)) {
            File workingDirectoryFile = Utils.join(Commands.CWD, fileName);
            File stagedFile = Utils.join(Commands.STAGING_DIRECTORY, fileName);
            Blob blob = null;
            if (stagedFile.exists()) {
                blob = Utils.readObject(stagedFile, Blob.class);
            }
            if (blobs.containsKey(fileName)) {
                if (!Arrays.equals(blobs.get(fileName).getFileContents(),
                        Utils.readContents(workingDirectoryFile))) {
                    if (!stagedFile.exists()) {
                        System.out.println(fileName + " (modified)");
                    }
                }
            } else if (stagedFile.exists() && blob.getAddition()) {
                if (!Arrays.equals(blob.getFileContents(),
                        Utils.readContents(workingDirectoryFile))) {
                    System.out.println(fileName + " (modified)");
                }
            }
        }
        for (String fileName : blobs.keySet()) {
            File workingDirectoryFile = Utils.join(Commands.CWD, fileName);
            File stagedFile = Utils.join(Commands.STAGING_DIRECTORY, fileName);
            Blob blob = null;
            if (stagedFile.exists()) {
                blob = Utils.readObject(stagedFile, Blob.class);
            }
            if (stagedFile.exists() && blob.getAddition()
                    && !workingDirectoryFile.exists()) {
                System.out.println(fileName + " (deleted)");
            } else if (!stagedFile.exists() || !blob.getRemoval()) {
                if (blobs.containsKey(fileName)
                        && !workingDirectoryFile.exists()) {
                    System.out.println(fileName + " (deleted)");
                }
            }
        }
    }
}


