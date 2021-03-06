package net.sf.cotta.memory;

import net.sf.cotta.PathContent;
import net.sf.cotta.PathSeparator;
import net.sf.cotta.TDirectory;
import net.sf.cotta.TDirectoryNotFoundException;
import net.sf.cotta.TFile;
import net.sf.cotta.TFileFactory;
import net.sf.cotta.TFileNotFoundException;
import net.sf.cotta.TIoException;
import net.sf.cotta.TPath;
import net.sf.cotta.TestCase;
import net.sf.cotta.io.OutputMode;
import net.sf.cotta.test.assertion.CodeBlock;
import net.sf.cotta.utils.PlatformInfoUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Date;
import java.util.List;

public abstract class InMemoryFileSystemTestBase extends TestCase {
  public TFileFactory factory;
  public InMemoryFileSystem fileSystem;

  public void beforeMethod() throws Exception {
    super.beforeMethod();
    fileSystem = createFileSystem();
    factory = new TFileFactory(fileSystem);
  }

  protected abstract InMemoryFileSystem createFileSystem();

  public void testToUri() {
    URI uri = fileSystem.toUri(TPath.parse("/test"));
    ensure.that(uri.getPath()).eq("/test");
  }

  public void testHaveBasicDirectoryExisting() throws Exception {
    ensure.that(fileSystem.dirExists(TPath.parse("/"))).eq(true);
    ensure.that(fileSystem.dirExists(TPath.parse("."))).eq(true);
  }

  public void testDoesNotHaveDrive() {
    ensure.that(fileSystem.dirExists(TPath.parse("C:\\"))).eq(false);
  }

  public void testInstantiateFileWithCorrectName() throws Exception {
    TFile file = factory.file("test");
    ensure.that(file).notNull();
    ensure.that(file.name()).eq("test");
  }

  public void testInstantiateAnNonExistingDirectoryWithCorrectName() throws Exception {
    TDirectory directory = factory.dir("test");
    ensure.that(directory.exists()).eq(false);
    ensure.that(directory.name()).eq("test");
  }

  public void testTakeAStringAsPath() throws Exception {
    TDirectory directory = factory.dir("C:\\Temp\\TfsTest");
    ensure.that(directory.name()).eq("TfsTest");
  }

  public void testMarkPathAndAllItsParentsAsExistsAfterCreation() throws TIoException {
    TPath path = TPath.parse("/tmp/subdirectory");
    fileSystem.createDir(path);
    ensure.that(fileSystem.dirExists(path)).eq(true);
    ensure.that(factory.dir("/tmp").exists()).eq(true);
  }

  public void testListAllSubDirectoriesCreated() throws Exception {
    fileSystem.createDir(TPath.parse("/tmp/one"));
    fileSystem.createDir(TPath.parse("/tmp/two"));
    List<TPath> actual = fileSystem.list(TPath.parse("/tmp")).dirs();
    ensure.set(actual).eq(TPath.parse("/tmp/one"), TPath.parse("/tmp/two"));
  }

  public void testListDirectoriesOnlyEvenWhenFileExists() throws Exception {
    TPath path = TPath.parse("/tmp");
    fileSystem.createDir(path);
    fileSystem.createDir(path.join("sub"));
    ensure.that(fileSystem.dirExists(path)).eq(true);
    fileSystem.createFile(path.join("file"));
    List<TPath> actual = fileSystem.list(path).dirs();
    ensure.that(actual).hasOneItem();
  }

  public void testNotCreateTheSamePathTwice() throws Exception {
    fileSystem.createDir(TPath.parse("/tmp/one"));
    try {
      fileSystem.createDir(TPath.parse("/tmp/one"));
      fail("IllegalArgumentException should have occurred");
    } catch (IllegalArgumentException e) {
      ensure.that(e).message().contains("/tmp/one");
      ensure.that(e).message().contains("exists");
    }
  }

  public void testCreateFileWithEmptyContent() throws Exception {
    TPath path = TPath.parse("/tmp/one.txt");
    fileSystem.createDir(path.parent());
    fileSystem.createFile(path);
    ensure.that(fileSystem.fileExists(path)).eq(true);
    ensure.that(fileSystem.dirExists(path)).eq(false);
    ensure.that(fileSystem.dirExists(path.parent())).eq(true);
  }

  public void testFailIfFileParentDoesNotExists() throws Exception {
    final TPath path = TPath.parse("/tmp/test.txt");
    ensure.code(new CodeBlock() {
      public void execute() throws Exception {
        fileSystem.createFile(path);
      }
    }).throwsException(TIoException.class);
  }

  public void testDeleteExistingFile() throws Exception {
    TPath path = TPath.parse("/tmp/file.txt");
    fileSystem.createDir(path.parent());
    fileSystem.createFile(path);
    ensure.that(fileSystem.fileExists(path)).eq(true);
    fileSystem.deleteFile(path);
    ensure.that(fileSystem.fileExists(path)).eq(false);
    PathContent content = fileSystem.list(path.parent());
    ensure.that(content.files()).isEmpty();
  }

  public void testThrowFileNotFoundExceptionIfFileToDeleteIsNotFound() throws Exception {
    TPath path = TPath.parse("/tmp/test.txt");
    try {
      fileSystem.deleteFile(path);
      fail("should throw FileNotFoundExcepiton");
    } catch (TFileNotFoundException e) {
      ensure.that(e).message().contains(path.toPathString());
    }
  }

  public void testBeAbleToLisInMemoryFilesInDirectory() throws Exception {
    TDirectory directory = factory.dir("tmp");
    directory.dir("subtmp").ensureExists();
    directory.file("file1").create();
    directory.file("file2").create();
    PathContent content = fileSystem.list(TPath.parse("tmp"));
    ensure.set(content.files()).eq(TPath.parse("tmp/file1"), TPath.parse("tmp/file2"));
  }

  public void testHaveRookDirectoriesExisting() throws Exception {
    ensure.that(fileSystem.dirExists(TPath.parse("/tmp").parent())).eq(true);
  }

  public void testCreateFileUnderRootDirectly() throws Exception {
    TPath path = TPath.parse("/test.txt");
    fileSystem.createFile(path);
    PathContent content = fileSystem.list(TPath.parse("/"));
    ensure.that(content.files()).eq(path);
  }

  public void testCreateDirUnderRoot() throws Exception {
    TPath path = TPath.parse("/directory");
    fileSystem.createDir(path);
    List<TPath> dirPaths = fileSystem.list(TPath.parse("/")).dirs();
    ensure.that(dirPaths).eq(path);
  }

  public void testNotAllowAFileCreationIfDirectoryOfTheSameNameExists() throws Exception {
    TPath path = TPath.parse("/tmp/a.b");
    fileSystem.createDir(path);
    try {
      fileSystem.createFile(path);
      fail("TIoException should have been thrown");
    } catch (TIoException e) {
      ensure.that(e).message().contains("directory");
    }
  }

  public void testNotAllowADirectoryCreationIfFileOfTheSameNameExists() throws Exception {
    TPath path = TPath.parse("/tmp/.confi");
    fileSystem.createDir(path.parent());
    fileSystem.createFile(path);
    try {
      fileSystem.createDir(path);
      fail("TIoException should have been thrown");
    } catch (TIoException e) {
      ensure.that(e).message().contains("file");
    }
  }

  public void testProvideInputStream() throws Exception {
    TPath path = TPath.parse("/tmp/input.txt");
    fileSystem.createDir(path.parent());
    fileSystem.createFile(path);
    InputStream is = fileSystem.createInputStream(path);
    registerResource(is);
    int expected = -1;
    ensure.that(is.read()).eq(expected);
  }

  public void testThrowExceptionIfFileNotFoundWhenCreatingInputStream() throws Exception {
    TPath path = TPath.parse("/tmp/input.txt");
    try {
      InputStream is = fileSystem.createInputStream(path);
      registerResource(is);
      fail("FileNotFoundException should have been thrown");
    } catch (TFileNotFoundException e) {
      ensure.that(e).message().contains(path.toPathString());
    }

  }

  public void testCreateOutputStreamAndCreateFile() throws Exception {
    TPath path = TPath.parse("/tmp/output.txt");
    fileSystem.createDir(path.parent());
    writeContent(path, OutputMode.OVERWRITE, "oops");
    ensure.that(fileSystem.dirExists(path.parent())).eq(true);
    PathContent content = fileSystem.list(path.parent());
    ensure.that(content.files()).eq(path);
    ensure.that(factory.file("/tmp/output.txt").load()).eq("oops");
  }

  private void writeContent(TPath path, OutputMode mode, String content) throws IOException {
    OutputStream os = fileSystem.createOutputStream(path, mode);
    registerResource(os);
    os.write(content.getBytes());
    os.close();
  }

  public void testCreateOutputStreamBasedOnTheMode() throws Exception {
    TPath path = TPath.parse("/tmp/bart.txt");
    fileSystem.createDir(path.parent());
    writeContent(path, OutputMode.APPEND, "one content");
    writeContent(path, OutputMode.APPEND, "two content");
    ensure.that(factory.file(path.toPathString()).load()).eq("one contenttwo content");
  }

  public void testOverwrite() throws Exception {
    TPath path = TPath.parse("/tmp/treasure.txt");
    fileSystem.createDir(path.parent());
    writeContent(path, OutputMode.APPEND, "content one");
    writeContent(path, OutputMode.OVERWRITE, "content two");
    ensure.that(factory.file(path.toPathString()).load()).eq("content two");
  }

  public void testAppendToFile() throws Exception {
    TPath path = TPath.parse("/tmp/filetoappend.txt");
    fileSystem.createDir(path.parent());
    OutputStream os1 = fileSystem.createOutputStream(path, OutputMode.APPEND);
    registerResource(os1);
    os1.write("one".getBytes());
    os1.close();
    OutputStream os2 = fileSystem.createOutputStream(path, OutputMode.APPEND);
    registerResource(os2);
    os2.write("two".getBytes());
    os2.close();
    ensure.that(loadContent(fileSystem, path)).eq("onetwo");
  }

  public void testThrowExceptionIfDirectoryToBeDeletedIsNotEmpty() throws Exception {
    TPath path = TPath.parse("/tmp/directory");
    fileSystem.createDir(path);
    try {
      fileSystem.deleteDirectory(path.parent());
      fail("TIoException should have been thrown");
    } catch (TIoException e) {
      ensure.that(e).message().contains(path.parent().toPathString());
    }
  }

  public void testDeleteDirectory() throws Exception {
    TPath path = TPath.parse("/tmp/directory");
    fileSystem.createDir(path);
    fileSystem.deleteDirectory(path);
    ensure.that(fileSystem.dirExists(path)).eq(false);
    ensure.that(fileSystem.list(path.parent()).dirs()).isEmpty();
  }

  public void testThrowExceptionIfDirectoryToDeleteIsNotThere() throws Exception {
    TPath path = TPath.parse("/tmp/test");
    try {
      fileSystem.deleteDirectory(path);
      fail("TDirectoryNotFoundException should have been thrown");
    } catch (TDirectoryNotFoundException e) {
      ensure.that(e).message().contains("/tmp/test");
    }
  }

  public void testMoveDirectly() throws Exception {
    InMemoryFileSystem fileSystem = new InMemoryFileSystem();
    TPath source = TPath.parse("/source.txt");
    TPath dest = TPath.parse("/dest.txt");
    String content = "in memory file";
    new TFile(new TFileFactory(fileSystem), source).save(content);
    long lastModified = fileSystem.fileLastModified(source);
    fileSystem.moveFile(source, dest);
    ensure.that(fileSystem.fileExists(source)).eq(false);
    ensure.that(fileSystem.fileExists(dest)).eq(true);
    ensure.that(loadContent(fileSystem, dest)).eq(content);
    ensure.that(fileSystem.fileLastModified(dest)).eq(lastModified);
  }

  private String loadContent(InMemoryFileSystem fileSystem, TPath path) throws TIoException {
    return new TFile(new TFileFactory(fileSystem), path).load();
  }

  public void testGetPathString() throws Exception {
    InMemoryFileSystem fileSystem = new InMemoryFileSystem();
    TPath source = TPath.parse("/source.txt");
    ensure.that(fileSystem.pathString(source)).eq(source.toPathString());
  }

  public void testGetFileLength() throws Exception {
    InMemoryFileSystem fileSystem = new InMemoryFileSystem();
    TPath path = TPath.parse("/content.txt");
    String content = "im memory file system";
    new TFile(new TFileFactory(fileSystem), path).save(content);
    ensure.that(fileSystem.fileLength(path)).eq(content.getBytes().length);
  }

  public void testGetFileLastModified() throws TIoException {
    InMemoryFileSystem fileSystem = new InMemoryFileSystem();
    TPath path = TPath.parse("/content.txt");
    String content = "im memory file system";
    Date date = new Date();
    new TFile(new TFileFactory(fileSystem), path).save(content);
    ensure.that(fileSystem.fileLength(path)).eq(content.getBytes().length);
    ensure.that(fileSystem.fileLastModified(path)).ge(date.getTime());
  }

  public void testThrowExceptionForParentNotFound() throws Exception {
    InMemoryFileSystem fileSystem = new InMemoryFileSystem();
    TPath path = TPath.parse("/dirnothere/test.txt");
    try {
      fileSystem.createOutputStream(path, OutputMode.OVERWRITE);
      fail("should have thrown exception");
    } catch (TIoException e) {
      ensure.that(e).message().contains("needs to be created first");
    }
  }

  public void testMoveDirectory() throws Exception {
    TPath source = TPath.parse("/source/file.txt");
    TPath target = TPath.parse("/target/file.txt");
    fileSystem.createDir(source.parent());
    ensure.that(fileSystem.dirExists(source.parent())).eq(true);
    writeContent(source, OutputMode.OVERWRITE, "path");
    fileSystem.moveDirectory(source.parent(), target.parent());
    ensure.that(loadContent(fileSystem, target)).eq("path");
  }

  public void testSupportPathSeparator() throws Exception {
    fileSystem = new InMemoryFileSystem(PathSeparator.Windows);
    ensure.that(fileSystem.pathString(TPath.parse("/one/two"))).eq("\\one\\two");
  }

  public void testListingFilesBasedOnTheListingOrder() throws TIoException {
    fileSystem = new InMemoryFileSystem(ListingOrder.AToZ);
    TPath a = TPath.parse("/dir/a");
    TPath z = TPath.parse("/dir/z");
    fileSystem.createDir(a.parent());
    fileSystem.createFile(a);
    fileSystem.createFile(z);
    List<TPath> actual = fileSystem.list(TPath.parse("/dir")).files();
    ensure.that(actual).eq(a, z);
    fileSystem = new InMemoryFileSystem(ListingOrder.ZToA);
    fileSystem.createDir(a.parent());
    fileSystem.createFile(a);
    fileSystem.createFile(z);
    actual = fileSystem.list(TPath.parse("/dir")).files();
    ensure.that(actual).eq(z, a);
  }

  public void testDontReturnNegagive1AsPartOfContentByte() throws IOException {
    fileSystem = new InMemoryFileSystem();
    TPath a = TPath.parse("/dir/a.txt");
    fileSystem.createDir(a.parent());
    OutputStream outputStream = fileSystem.createOutputStream(a, OutputMode.OVERWRITE);
    outputStream.write(-1);
    outputStream.close();
    InputStream inputStream = fileSystem.createInputStream(a);
    ensure.that(inputStream.read()).eq(255);
  }

  public void testCompareToComparesPath() {
    fileSystem = new InMemoryFileSystem();
    TPath a = TPath.parse("a");
    TPath b = TPath.parse("b");
    ensure.that(fileSystem.compare(a, b)).eq(a.compareTo(b));

  }

}
