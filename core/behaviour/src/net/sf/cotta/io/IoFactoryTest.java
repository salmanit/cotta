package net.sf.cotta.io;

import net.sf.cotta.TIoException;
import net.sf.cotta.test.TestCase;
import org.jmock.Expectations;
import org.jmock.Mockery;

import java.io.*;

@SuppressWarnings({"deprecation"})
public class IoFactoryTest extends TestCase {
  private Mockery context;

  public void beforeMethod() throws Exception {
    context = new Mockery();
  }

  public void afterMethod() throws Exception {
    context.assertIsSatisfied();
  }

  public void testCreateInputStream() throws Exception {
    final InputStreamStub stub = new InputStreamStub();
    final StreamFactory factory = mockFactoryForInput(stub);
    ensure.that(new IoFactory(factory).inputStream()).sameAs(stub);
  }

  private StreamFactory mockFactoryForInput(final InputStream stub) throws TIoException {
    Mockery context = new Mockery();
    final StreamFactory factory = context.mock(StreamFactory.class);
    context.checking(new Expectations() {
      {
        one(factory).inputStream();
        will(returnValue(stub));
      }
    });
    return factory;
  }

  public void testCreateOutputStream() throws Exception {
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    final StreamFactory factory = mockFactoryForOutput(output);
    ensure.that(new IoFactory(factory).outputStream(OutputMode.OVERWRITE)).sameAs(output);
  }

  private StreamFactory mockFactoryForOutput(final ByteArrayOutputStream output) throws TIoException {
    final StreamFactory factory = context.mock(StreamFactory.class);
    context.checking(new Expectations() {
      {
        one(factory).outputStream(OutputMode.OVERWRITE);
        will(returnValue(output));
      }
    });
    return factory;
  }

  public void testCreateReader() throws Exception {
    final InputStreamStub stub = new InputStreamStub();
    final StreamFactory streamFactory = mockFactoryForInput(stub);
    Reader reader = new IoFactory(streamFactory).reader();
    reader.close();
    ensure.that(stub.isClosed()).eq(true);
  }

  public void testCreateReaderWithEncoding() throws Exception {
    String value = "\u00c7\u00c9";
    ByteArrayInputStream inputStream = new ByteArrayInputStream(value.getBytes("utf-16"));
    final StreamFactory streamFactory = mockFactoryForInput(inputStream);
    Reader reader = new IoFactory(streamFactory).reader("utf-16");
    char[] ch = new char[2];
    reader.read(ch, 0, 2);
    reader.close();
    ensure.that(new String(ch)).eq(value);
  }

  public void testCreateBufferredReader() throws Exception {
    final InputStreamStub stub = new InputStreamStub();
    final StreamFactory streamFactory = mockFactoryForInput(stub);
    IoFactory factory = new IoFactory(streamFactory);
    factory.bufferedReader().close();
    ensure.that(stub.isClosed()).eq(true);
  }

  public void testCreateLineBufferedReader() throws Exception {
    final InputStreamStub stub = new InputStreamStub();
    final StreamFactory streamFactory = mockFactoryForInput(stub);
    IoFactory factory = new IoFactory(streamFactory);
    factory.lineNumberReader().close();
    ensure.that(stub.isClosed()).eq(true);
  }

  public void testCreateWriter() throws Exception {
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    final StreamFactory streamFactory = mockFactoryForOutput(output);
    IoFactory factory = new IoFactory(streamFactory);
    Writer writer = factory.writer(OutputMode.OVERWRITE);
    writer.write("content".toCharArray());
    writer.close();
    ensure.that(output.toString()).eq("content");
  }

  public void testCreateWriterWithEncoding() throws Exception {
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    final StreamFactory streamFactory = mockFactoryForOutput(output);
    IoFactory factory = new IoFactory(streamFactory);
    Writer writer = factory.writer(OutputMode.OVERWRITE, "utf-8");
    writer.write("\u00c7\u00c9".toCharArray());
    writer.close();
    ensure.that(output.toString("utf-8")).eq("\u00c7\u00c9");
  }

  public void testCreateWriterUsingDefaultEncoding() throws Exception {
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    final StreamFactory streamFactory = mockFactoryForOutput(output);
    IoFactory factory = new IoFactory(streamFactory, "utf-8");
    Writer writer = factory.writer(OutputMode.OVERWRITE);
    writer.write("\u00c7\u00c9".toCharArray());
    writer.close();
    ensure.that(output.toString("utf-8")).eq("\u00c7\u00c9");
  }

  public void testCreatePrintWriter() throws Exception {
    final ByteArrayOutputStream output = new ByteArrayOutputStream();
    final StreamFactory streamFactory = mockFactoryForOutput(output);
    IoFactory factory = new IoFactory(streamFactory);
    PrintWriter printer = factory.printWriter(OutputMode.OVERWRITE);
    printer.print("number");
    printer.close();
    ensure.that(output.toString()).eq("number");
    context.assertIsSatisfied();
  }

  public void testCreateLineNumberReader() throws Exception {
    InputStreamStub stub = new InputStreamStub();
    StreamFactory factory = mockFactoryForInput(stub);
    LineNumberReader reader = new IoFactory(factory).lineNumberReader();
    reader.close();
    ensure.that(stub.isClosed()).eq(true);
  }

}
