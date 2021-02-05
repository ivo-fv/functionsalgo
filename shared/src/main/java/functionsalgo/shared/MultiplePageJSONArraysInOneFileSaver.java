package functionsalgo.shared;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class MultiplePageJSONArraysInOneFileSaver {

    private File jsonFile;

    public MultiplePageJSONArraysInOneFileSaver(File jsonFileToSaveTo) throws IOException {
        jsonFile = jsonFileToSaveTo;
        // beginning [ of the array
        try (FileOutputStream output = new FileOutputStream(jsonFile, false)) {
            output.getChannel().write(ByteBuffer.wrap("[".getBytes(StandardCharsets.UTF_8)));
        }
    }

    public void append(InputStream input) throws IOException {
        try (FileOutputStream output = new FileOutputStream(jsonFile, true)) {
            byte[] buffer = new byte[1024];
            int length = 0;
            // the first [ of the page shouldn't be written
            if ((length = input.read(buffer)) > 0) {
                output.write(buffer, 1, length - 1);
            }
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
            // the last ] of the page will get overwritten with a ,
            output.getChannel().write(ByteBuffer.wrap(",".getBytes(StandardCharsets.UTF_8)),
                    output.getChannel().size() - 1);
        }
    }

    public void finish() throws IOException {
        // replace the , at the end with ] to finish the job
        try (FileOutputStream output = new FileOutputStream(jsonFile, true)) {
            output.getChannel().write(ByteBuffer.wrap("]".getBytes(StandardCharsets.UTF_8)),
                    output.getChannel().size() - 1);
            output.flush();
        }
    }

}
