package net.sf.cotta;

import net.sf.cotta.io.*;
import net.sf.cotta.system.FileSystem;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * The class that represents the file.  Even though the constructor is public, the usual
 * way is to create TFile through TFile, TDirectory, and TFileFactory
 *
 * @see TFileFactory#file(String)
 * @see TFileFactory#fileFromJavaFile(java.io.File)
 * @see TDirectory#file(String)
 * @see TDirectory#file(TPath)
 */
public class TFile extends TEntry {

  /**
   * Create TFile instance backed up by the file system
   *
   * @param fileSystem file system backing the file
   * @param path       path for the file
   * @see #TFile(TFileFactory, TPath)
   * @see #TFile(TFileFactory, TPath)
   * @deprecated use the other constructor for default encoding support provided by factory
   */
  public TFile(FileSystem fileSystem, TPath path) {
    this(new TFileFactory(fileSystem), path);
  }

  /**
   * Create TFile instance backed up by the factory
   *
   * @param factory file factory as the file system
   * @param path    path for the file
   */
  public TFile(TFileFactory factory, TPath path) {
    super(factory, path);
  }

  public boolean exists() {
    return filesystem().fileExists(path);
  }

  public TFile create() throws TIoException {
    parent().ensureExists();
    filesystem().createFile(path);
    return this;
  }

  public String extname() {
    String name = name();
    int index = name.lastIndexOf('.');
    return index == -1 ? "" : name.substring(index + 1);
  }

  public String basename() {
    String name = name();
    int index = name.lastIndexOf('.');
    return index == -1 ? name : name.substring(0, index);
  }

  public void delete() throws TIoException {
    filesystem().deleteFile(path);
  }

  private InputStreamFactory inputStreamFactory() {
    return new InputStreamFactory() {
      public InputStream inputStream() throws TIoException {
        return TFile.this.inputStream();
      }

      public FileChannel inputChannel() throws TIoException {
        return TFile.this.inputChannel();
      }

      public TPath path() {
        return TFile.this.toPath();
      }
    };
  }

  private OutputStreamFactory outputStreamFactory(final OutputMode mode) {
    return new OutputStreamFactory() {
      public OutputStream outputStream() throws TIoException {
        return TFile.this.outputStream(mode);
      }

      public TPath path() {
        return TFile.this.toPath();
      }
    };
  }

  @SuppressWarnings({"deprecation"})
  private StreamFactory streamFactory() {
    return new StreamFactory() {
      public InputStream inputStream() throws TIoException {
        return TFile.this.inputStream();
      }

      public FileChannel inputChannel() throws TIoException {
        return TFile.this.inputChannel();
      }

      public OutputStream outputStream(OutputMode mode) throws TIoException {
        return TFile.this.outputStream(mode);
      }

      public TPath path() {
        return path;
      }

    };
  }

  /**
   * Creates the input channel for the file
   *
   * @return FileChannel for input
   * @throws TIoException error in creating the input channel
   */
  public FileChannel inputChannel() throws TIoException {
    return filesystem().createInputChannel(path);
  }

  /**
   * Create the output stream
   *
   * @param mode output mode
   * @return output stream for the file
   * @throws TIoException error in creating the output stream
   */
  public OutputStream outputStream(OutputMode mode) throws TIoException {
    parent().ensureExists();
    return filesystem().createOutputStream(path, mode);
  }

  /**
   * Create the input stream
   *
   * @return input stream for the file
   * @throws TIoException error in creating the input stream
   */
  public InputStream inputStream() throws TIoException {
    return filesystem().createInputStream(path);
  }

  public void copyTo(final TFile target) throws TIoException {
    target.write(new OutputProcessor() {
      public void process(OutputManager manager) throws IOException {
        copyTo(manager.outputStream());
      }
    });
  }

  public void copyTo(final OutputStream outputStream) throws TIoException {
    read(new InputProcessor() {
      public void process(InputManager inputManager) throws IOException {
        copy(inputManager.inputStream(), outputStream);
      }
    });
  }

  private void copy(InputStream is, OutputStream os) throws IOException {
    byte[] buffer = new byte[256];
    int read = is.read(buffer, 0, buffer.length);
    while (read > -1) {
      os.write(buffer, 0, read);
      read = is.read(buffer, 0, buffer.length);
    }
  }

  public void moveTo(TFile destination) throws TIoException {
    if (!exists()) {
      throw new TFileNotFoundException(path);
    }
    if (destination.exists()) {
      throw new TIoException(destination.path, "Destination exists");
    }
    if (filesystem() == destination.filesystem() || filesystem().equals(destination.filesystem())) {
      filesystem().moveFile(this.path, destination.path);
    } else {
      this.copyTo(destination);
      delete();
    }
  }

  public long length() {
    return filesystem().fileLength(path);
  }

  public long lastModified() {
    return filesystem().fileLastModified(path);
  }

  public TFile ensureExists() throws TIoException {
    if (!exists()) {
      create();
    }
    return this;
  }

  /**
   * Returns the IoFactory backed by the current TFile
   *
   * @return IoFactory
   * @see #inputChannel()
   * @see #inputStream()
   * @see #outputStream(net.sf.cotta.io.OutputMode)
   * @deprecated use TFile itself is more effective
   */
  @SuppressWarnings({"deprecation"})
  @Deprecated
  public IoFactory io() {
    return new IoFactory(streamFactory(), factory().defaultEncoding());
  }

  /**
   * Open the file for I/O processing
   *
   * @param processor processor call back
   * @throws TIoException error in the processing
   * @see #read(net.sf.cotta.io.InputProcessor)
   * @see #write(net.sf.cotta.io.OutputProcessor)
   * @see #append(net.sf.cotta.io.OutputProcessor)
   * @deprecated use read(), write(), and append() instead
   */
  @SuppressWarnings({"deprecation"})
  @Deprecated
  public void open(IoProcessor processor) throws TIoException {
    new IoManager(streamFactory(), factory().defaultEncoding()).open(processor);
  }

  /**
   * Read the file with a input processor
   *
   * @param processor processor for the input stream
   * @throws TIoException error in reading the file
   */
  public void read(final InputProcessor processor) throws TIoException {
    Input.with(inputStreamFactory()).read(processor);
  }

  /**
   * Read the file with a line processor
   *
   * @param lineProcessor line processor for the lines
   * @throws TIoException error in reading the file
   */
  public void read(final LineProcessor lineProcessor) throws TIoException {
    Input.with(inputStreamFactory()).readLines(lineProcessor);
  }

  public void append(final OutputProcessor processor) throws TIoException {
    Output.with(outputStreamFactory(OutputMode.APPEND), null).write(processor);
  }

  public void write(final OutputProcessor processor) throws TIoException {
    Output.with(outputStreamFactory(OutputMode.OVERWRITE), null).write(processor);
  }

  /**
   * Read the file with a line processor
   *
   * @param lineProcessor line processor for the lines
   * @throws TIoException error in reading the file
   * @see #read(net.sf.cotta.io.LineProcessor)
   * @deprecated use #read(LineProcessor)
   */
  @Deprecated
  public void open(final LineProcessor lineProcessor) throws TIoException {
    read(lineProcessor);
  }

  /**
   * Load the content of the file into string using system default encoding
   *
   * @return content of the file
   * @throws TIoException error in reading the file
   */
  public String load() throws TIoException {
    return Input.with(streamFactory()).load();
  }

  /**
   * Saves the content to the file
   *
   * @param content content to save
   * @return the file instance
   * @throws TIoException if there are any exception thrown during the operation
   */
  public TFile save(final String content) throws TIoException {
    write(new OutputProcessor() {
      public void process(OutputManager io) throws IOException {
        Writer writer = io.writer();
        writer.write(content);
        writer.flush();
      }
    });
    return this;
  }

  /**
   * Parse the file into an object
   *
   * @param parser parser to call after opening the file for read
   * @param <T>    the type of the object to return
   * @return the parsed object
   * @throws TIoException error in reading the file
   */
  public <T> T parse(final Parser<T> parser) throws TIoException {
    final List<T> result = new ArrayList<T>(1);
    read(new InputProcessor() {
      public void process(InputManager inputManager) throws IOException {
        result.add(parser.parse(inputManager));
      }
    });
    return result.get(0);
  }

  /**
   * Converts to the instance with a cononical path
   *
   * @return the instance with a cononical path
   */
  public TFile toCanonicalFile() {
    return factory().file(toCanonicalPath());
  }

  public URI toUri() {
    return filesystem().toUri(toPath());
  }

  public URL toUrl() throws MalformedURLException {
    return toUri().toURL();
  }
}