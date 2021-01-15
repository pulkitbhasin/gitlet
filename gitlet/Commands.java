package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;
import java.util.Stack;

/** Fundamental commands.
 * @author pulkit  */
public class Commands {

    /** Current working directory. */
    static final File CWD = new File(".");
    /** Gitlet folder. */
    static final File GITLET_FOLDER = Utils.join(CWD, ".gitlet");
    /** Staging directory. */
    static final File STAGING_DIRECTORY = Utils.join(GITLET_FOLDER,
            "staging_directory");
    /** Current branch. */
    static final File HEAD = Utils.join(GITLET_FOLDER, "head.txt");
    /** All branches. */
    static final File BRANCHES = Utils.join(GITLET_FOLDER, "branches");
    /** All commits. */
    static final File COMMITS = Utils.join(GITLET_FOLDER, "commits");
    /** All blobs. */
    static final File OBJECTS = Utils.join(GITLET_FOLDER, "objects");
    /** All remotes. */
    static final File REMOTES = Utils.join(GITLET_FOLDER, "remote");

    /** Initialize gitlet directory. */
    public static void init() throws IOException {
        if (GITLET_FOLDER.exists()) {
            exitWithError("A Gitlet version-control "
                    + "system already exists in the "
                    + "current directory.");
        }
        GITLET_FOLDER.mkdir();
        STAGING_DIRECTORY.mkdir();
        BRANCHES.mkdir();
        COMMITS.mkdir();
        OBJECTS.mkdir();
        HEAD.createNewFile();
        REMOTES.mkdir();
        Date epochDate = new Date(0);
        Commit initialCommit = new Commit("initial commit",
                epochDate, null,
                null);
        Branch masterBranch = new Branch(initialCommit, "master");
        Branch.setCurrentBranch(masterBranch);
    }

    /** Stage file for addition.
     * @param fileName */
    public static void add(String fileName) throws IOException {
        File file = Utils.join(CWD, fileName);
        if (!file.exists()) {
            exitWithError("File does not exist");
        }
        File fileStaged = Utils.join(STAGING_DIRECTORY, fileName);
        if (!fileStaged.exists()) {
            fileStaged.createNewFile();
        }
        Commit currentCommit = Branch.getCurrentBranch().getHeadCommit();
        Hashtable<String, Blob> blobs = currentCommit.getBlobs();
        Blob blob = new Blob(fileName);
        if (blobs.containsKey(blob.getName())) {
            Blob blobTracked = blobs.get(blob.getName());
            if (blob.getID().equals(blobTracked.getID())) {
                blob.setAddition(false);
                blob.setRemoval(false);
                fileStaged.delete();
            } else {
                blob.setAddition(true);
                blob.setRemoval(false);
                Utils.writeObject(fileStaged, blob);
            }
        } else {
            blob.setAddition(true);
            blob.setRemoval(false);
            Utils.writeObject(fileStaged, blob);
        }
    }

    /** Create new commit.
     * @param message  */
    public static void commit(String message) throws IOException {
        if (message.trim().equals("")) {
            exitWithError("Please enter a commit message");
        }
        if (STAGING_DIRECTORY.list().length == 0) {
            exitWithError("No changes added to the commit.");
        }
        Commit currentCommit = Branch.getCurrentBranch().getHeadCommit();
        Commit newCommit = new Commit(message, new Date(), currentCommit, null);
        Branch currentBranch = Branch.getCurrentBranch();
        currentBranch.setHeadCommit(newCommit);
    }

    /** Create new merged commit.
     * @param firstParent "first parent"
     * @param message */
    public static void commit(String message,
                              Commit firstParent) throws IOException {
        if (message.trim().equals("")) {
            exitWithError("Please enter a commit message");
        }
        if (STAGING_DIRECTORY.list().length == 0) {
            exitWithError("No changes added to the commit.");
        }
        Commit currentCommit = Branch.getCurrentBranch().getHeadCommit();
        Commit newCommit = new Commit(message, new Date(),
                currentCommit, firstParent);
        Branch currentBranch = Branch.getCurrentBranch();
        currentBranch.setHeadCommit(newCommit);
    }

    /** Stage file for removal.
     * @param fileName  */
    public static void rm(String fileName) throws IOException {
        File stagedFile = Utils.join(STAGING_DIRECTORY, fileName);
        boolean flag = false;
        if (stagedFile.exists()) {
            flag = true;
            Blob blob = Utils.readObject(stagedFile, Blob.class);
            if (blob.getAddition()) {
                blob.setAddition(false);
                stagedFile.delete();
            }
        }
        Branch currentBranch = Branch.getCurrentBranch();
        Commit currentCommit = currentBranch.getHeadCommit();
        Hashtable<String, Blob> blobs = currentCommit.getBlobs();
        if (blobs.get(fileName) != null) {
            flag = true;
            File stagedRemovalFile =  Utils.join(STAGING_DIRECTORY, fileName);
            stagedRemovalFile.createNewFile();
            Blob blobToRemove = blobs.get(fileName);
            blobToRemove.setRemoval(true);
            Utils.writeObject(stagedRemovalFile, blobToRemove);
            File currentFile = Utils.join(CWD, fileName);
            currentFile.delete();
            Branch.setCurrentBranch(currentBranch);
        }
        if (!flag) {
            exitWithError("No reason to remove the file.");
        }
    }

    /** Merge two branches.
     * @param branchName */
    public static void merge(String branchName) throws IOException {
        File file = Utils.join(BRANCHES, branchName + ".txt");
        boolean conflict = false;
        if (!file.exists()) {
            exitWithError("A branch with that name does not exist.");
        } else if (STAGING_DIRECTORY.list().length != 0) {
            exitWithError("You have uncommitted changes.");
        }
        Branch branch = Utils.readObject(file, Branch.class);
        Branch currentBranch = Branch.getCurrentBranch();
        Commit branchHeadCommit = branch.getHeadCommit();
        Commit currentBranchHeadCommit = currentBranch.getHeadCommit();
        Hashtable<String, Blob> currentBranchBlobs =
                currentBranchHeadCommit.getBlobs();
        Hashtable<String, Blob> branchBlobs = branchHeadCommit.getBlobs();
        for (String fileName : Commands.CWD.list()) {
            File file2 = Utils.join(Commands.CWD, fileName);
            if (!currentBranchBlobs.containsKey(fileName)
                    && branchBlobs.containsKey(fileName)
                    && !Arrays.equals(
                            branchBlobs.get(fileName).getFileContents(),
                    Utils.readContents(file2))) {
                Commands.exitWithError("There is an"
                        + " untracked file in the way; delete it, "
                        + "or add and commit it first.");
            }
        }
        if (branchName.equals(currentBranch.getName())) {
            exitWithError("Cannot merge a branch with itself.");
        }
        Commit splitPoint;
        List<Object> lst = splitPointFinder(currentBranch, branch);
        List<Object> lst1 = splitPointFinder(branch, currentBranch);
        if ((Integer) lst.get(0) < (Integer) lst1.get(0)) {
            splitPoint = (Commit) lst.get(1);
        } else {
            splitPoint = (Commit) lst1.get(1);
        }
        File splitFile = Utils.join(Commands.COMMITS, "splitCommit.txt");
        Utils.writeObject(splitFile, splitPoint);
        Hashtable<String, Blob> splitCommitBlobs = splitPoint.getBlobs();
        if (splitPoint.getName().equals(branchHeadCommit.getName())) {
            exitWithError("Given branch is an ancestor of the current branch.");
        } else if (splitPoint.getName().equals
                (currentBranchHeadCommit.getName())) {
            BranchCommands.checkoutThree(branchName);
            exitWithError("Current branch fast-forwarded.");
        }
        conflict = mergeHelper(splitCommitBlobs,
                currentBranchBlobs, branchBlobs, branchHeadCommit);
        String message = "Merged "
                + branchName + " into "
                + currentBranch.getName() + ".";
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
        commit(message, branchHeadCommit);
    }

    /** Error handling.
     * @param message */
    public static void exitWithError(String message) {
        if (message != null && !message.equals("")) {
            System.out.println(message);
        }
        System.exit(0);
    }

    /** Steps to execute if merge conflict detected.
     * @param b "b"
     * @param c "c"
     * @param f */
    public static void conflict(String f,
                                Hashtable<String, Blob> c,
                                Hashtable<String, Blob> b) throws IOException {
        String lst = "";
        File file1 = Utils.join(CWD, f);
        lst += "<<<<<<< HEAD\n";
        if (c != null) {
            Utils.writeContents(file1, (Object) c.get(f).getFileContents());
            lst += Utils.readContentsAsString(file1);
        }
        lst += ("=======\n");
        if (b != null) {
            Utils.writeContents(file1, (Object) b.get(f).getFileContents());
            lst += Utils.readContentsAsString(file1);
        }
        lst += (">>>>>>>\n");
        Utils.writeContents(file1, lst);
        add(f);
    }

    /** Finding split point for merge.
     * @param branch "branch"
     * @param branch1 "branch1"
     * @return "list" */
    public static List<Object> splitPointFinder(Branch branch, Branch branch1) {
        List<Object> lst = new ArrayList<>();
        int total = 0;
        Commit splitPoint = null;
        Commit branchMergedCommit = null;
        Commit branchCommit = branch.getHeadCommit();
        while (splitPoint == null) {
            Stack<Commit> fringe = new Stack<>();
            fringe.push(branch1.getHeadCommit());
            while (!fringe.isEmpty()) {
                Commit commit = fringe.pop();
                if (commit.getName().equals(branchCommit.getName())) {
                    splitPoint = commit;
                    break;
                } else if (branchMergedCommit != null) {
                    if (commit.getName().equals(branchMergedCommit.getName())) {
                        splitPoint = commit;
                        break;
                    }
                } else {
                    if (commit.getParent() != null) {
                        fringe.push(commit.getParent());
                    }
                    if (commit.getMergedParent() != null) {
                        fringe.push(commit.getMergedParent());
                    }
                }
                total++;
            }
            branchCommit = branchCommit.getParent();
            if (branchCommit != null) {
                branchMergedCommit = branchCommit.getMergedParent();
            }
            total++;
        }
        lst.add(total);
        lst.add(splitPoint);
        return lst;
    }

    /** merge helper.
     * @param splitCommit "split commit blobs"
     * @param branchBlobs "branch blobs"
     * @param commit  "branch head commit"
     * @param currentBranch "current branch blobs"
     * @return */
    private static boolean mergeHelper(Hashtable<String, Blob> splitCommit,
                                       Hashtable<String, Blob> currentBranch,
                                       Hashtable<String, Blob> branchBlobs,
                                       Commit commit) throws IOException {
        boolean conflict = false;
        Commit branchHeadCommit = commit;
        Hashtable<String, Blob> currentBranchBlobs = currentBranch;
        Hashtable<String, Blob> splitCommitBlobs = splitCommit;
        Hashtable<String, Blob> urjasvi = splitCommitBlobs;
        for (String fileName : splitCommitBlobs.keySet()) {
            if (branchBlobs.get(fileName) != null
                    && currentBranchBlobs.get(fileName) != null) {
                if (!Arrays.equals(branchBlobs.get(fileName).getFileContents(),
                        splitCommitBlobs.get(fileName).getFileContents())) {
                    if (Arrays.equals
                            (currentBranchBlobs.get(fileName).getFileContents(),
                                    urjasvi.get(fileName).getFileContents())) {
                        String pulkit = branchHeadCommit.getName();
                        BranchCommands.checkoutTwo(pulkit, fileName);
                        add(fileName);
                    } else {
                        conflict = true;
                        conflict(fileName, currentBranchBlobs, branchBlobs);
                    }
                }
            } else if (currentBranchBlobs.get(fileName) != null) {
                if (Arrays.equals(currentBranchBlobs.get
                                (fileName).getFileContents(),
                        splitCommitBlobs.get(fileName).getFileContents())) {
                    rm(fileName);
                } else {
                    conflict = true;
                    conflict(fileName, currentBranchBlobs, null);
                }
            } else if (branchBlobs.get(fileName) != null) {
                if (!Arrays.equals(branchBlobs.get(fileName).getFileContents(),
                        splitCommitBlobs.get(fileName).getFileContents())) {
                    conflict = true;
                    conflict(fileName, null, branchBlobs);
                }
            }
        }
        for (String fileName : branchBlobs.keySet()) {
            Blob hey = branchBlobs.get(fileName);
            if (!splitCommitBlobs.containsKey(fileName)) {
                if (!currentBranchBlobs.containsKey(fileName)) {
                    String hi = branchHeadCommit.getName();
                    BranchCommands.checkoutTwo(hi, fileName);
                    add(fileName);
                } else if (!Arrays.equals(hey.getFileContents(),
                        currentBranchBlobs.get(fileName).getFileContents())) {
                    conflict = true;
                    conflict(fileName, currentBranchBlobs, branchBlobs);
                }
            }
        }
        return conflict;
    }
}
