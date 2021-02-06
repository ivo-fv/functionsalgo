package functionsalgo.shared;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;

public class MultiplePageJSONInOneFileSaver {

    private File jsonFile;
    private long lenWritten = 0;

    public MultiplePageJSONInOneFileSaver(File jsonFileToSaveTo, boolean appendToExisting) throws IOException {
        jsonFile = jsonFileToSaveTo;

        if (appendToExisting && jsonFileToSaveTo.exists()) {
            try (FileChannel ch = FileChannel.open(jsonFile.toPath(), StandardOpenOption.WRITE)) {
                lenWritten = ch.size();
                // the last ] of the page will get overwritten with a ,
                ch.write(ByteBuffer.wrap(",".getBytes(StandardCharsets.UTF_8)), lenWritten - 1);
            }
        } else {
            // beginning [ of the array
            try (FileOutputStream output = new FileOutputStream(jsonFile, false)) {
                output.getChannel().write(ByteBuffer.wrap("[".getBytes(StandardCharsets.UTF_8)));
                lenWritten += 1;
            }
        }
    }

    public void appendArray(InputStream input) throws IOException {

        try (FileChannel ch = FileChannel.open(jsonFile.toPath(), StandardOpenOption.WRITE)) {
            byte[] buffer = new byte[1024];
            int length = 0;
            // the first [ of the page shouldn't be written
            if ((length = input.read(buffer)) > 0) {
                ch.write(ByteBuffer.wrap(buffer, 1, length - 1), lenWritten);
                lenWritten += length - 1;
            }

            while ((length = input.read(buffer)) > 0) {
                ch.write(ByteBuffer.wrap(buffer, 0, length), lenWritten);
                lenWritten += length;
            }
            // the last ] of the page will get overwritten with a ,
            ch.write(ByteBuffer.wrap(",".getBytes(StandardCharsets.UTF_8)), lenWritten - 1);
        }
    }
    
    public void appendObject(InputStream input) throws IOException {

        try (FileChannel ch = FileChannel.open(jsonFile.toPath(), StandardOpenOption.WRITE)) {
            byte[] buffer = new byte[1024];
            int length = 0;
            
            while ((length = input.read(buffer)) > 0) {
                ch.write(ByteBuffer.wrap(buffer, 0, length), lenWritten);
                lenWritten += length;
            }
            // prepare the next object of the array
            ch.write(ByteBuffer.wrap(",".getBytes(StandardCharsets.UTF_8)), lenWritten);
            lenWritten += 1;
        }
    }

    public void finish() throws IOException {
        // replace the , at the end with ] to finish the job
        try (FileChannel ch = FileChannel.open(jsonFile.toPath(), StandardOpenOption.WRITE)) {
            ch.write(ByteBuffer.wrap("]".getBytes(StandardCharsets.UTF_8)), lenWritten - 1);
            ch.force(false);
        }
    }
}
