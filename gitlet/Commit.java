package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

/** Commit object.
 * @author pulkit  */

public class Commit implements Serializable {

    /** Stores commit name. */
    private String _name;
    /** Stores commit parent. */
    private Commit _parent;
    /** Stores commit message. */
    private String _message;
    /** Stores commit date. */
    private String _date;
    /** Maps blob name to blob object. */
    private Hashtable<String, Blob> _blobs;
    /** Stores merged parent. */
    private Commit _firstParent;

    /** Constructor.
     * @param message "message"
     * @param date  "date"
     * @param firstParent "merged parent"
     * @param parent */
    Commit(String message,
           Date date,
           Commit parent,
           Commit firstParent)
            throws IOException {
        _message = message;
        _firstParent = firstParent;
        DateFormat dateFormat = new SimpleDateFormat("E MMM d HH:mm:ss YYYY Z");
        _date = dateFormat.format(date);
        _parent = parent;
        _blobs = new Hashtable<>();
        if (parent != null) {
            for (String key : _parent.getBlobs().keySet()) {
                _blobs.put(key, _parent.getBlobs().get(key));
            }
        }
        for (String fileName : Commands.STAGING_DIRECTORY.list()) {
            File file = Utils.join(Commands.STAGING_DIRECTORY, fileName);
            Blob blob = Utils.readObject(file, Blob.class);
            String blobName = blob.getName();
            if (blob.getAddition()) {
                if (_blobs.contains(blobName)) {
                    _blobs.remove(blobName);
                }
                blob.setAddition(false);
                _blobs.put(blobName, blob);
                file.delete();
            } else if (blob.getRemoval()) {
                if (_blobs.containsKey(blobName)) {
                    _blobs.remove(blobName);
                }
                blob.setRemoval(false);
                file.delete();
            }
        }
        List<Object> values = new ArrayList<>();
        for (String key : _blobs.keySet()) {
            values.add(_blobs.get(key).getFileContents());
        }
        values.add(_message);
        values.add(_date);
        _name = Utils.sha1(values);
        File commitFile = Utils.join(Commands.COMMITS, _name + ".txt");
        commitFile.createNewFile();
        Utils.writeObject(commitFile, this);
    }

    /** Constructor for remote commits.
     * @param date "date"
     * @param parent "parent"
     * @param message "message"
     * @param blobs "blobs"
     * @param name */
    Commit(String name,
           String message,
           String date,
           Commit parent,
           Hashtable<String, Blob> blobs)
            throws IOException {
        _message = message;
        _date = date;
        _parent = parent;
        _blobs = blobs;
        _firstParent = null;
        _name = name;
        File commitFile = Utils.join(Commands.COMMITS, _name + ".txt");
        commitFile.createNewFile();
        Utils.writeObject(commitFile, this);
    }


    /** Get blobs.
     * @return  */
    public Hashtable<String, Blob> getBlobs() {
        return _blobs;
    }

    /** Get commit name.
     * @return  */
    public String getName() {
        return _name;
    }

    /** Get parent.
     * @return  */
    public Commit getParent() {
        return _parent;
    }

    /** Get message.
     * @return  */
    public String getMessage() {
        return _message;
    }

    /** Get merged parent.
     * @return  */
    public Commit getMergedParent() {
        return _firstParent;
    }

    /** Get date.
     * @return  */
    public String getDate() {

        return _date;
    }

    /** Print commit details. */
    public void print() {
        System.out.println("===");
        System.out.println("commit " + getName());
        if (_firstParent != null) {
            System.out.println("Merge: "
                    + _parent.getName().substring(0, 7)
                    + " " + _firstParent.getName().substring(0, 7) + " ");
        }
        System.out.println("Date: " + _date);
        System.out.println(_message);
        System.out.println();
    }
}
