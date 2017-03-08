
package org.restnext.core.classpath;

import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;

/**
 * Created by thiago on 25/11/16.
 */
public class ClasspathRegisterTest {

  //@Rule
  //public TemporaryFolder folder = new TemporaryFolder(new File(System.getProperty("user.dir")));

  //@Test
  //public void addPathTest() {
  //  //final Path path = folder.newFile("teste.jar").toPath();
  //  final Path path = Paths.get(".");
  //  assertTrue(Arrays.stream(getClassPath().split(":")).map(filepath -> Paths.get(filepath))
  //      .noneMatch(file -> file.equals(path)));
  //  ClasspathRegister.addPath(path);
  //  assertTrue(Arrays.stream(getClassPath().split(":")).map(filepath -> Paths.get(filepath))
  //      .anyMatch(file -> file.equals(path)));
  //}

  private static String getClassPath() {
    final String key = "java.class.path";
    return System.getSecurityManager() == null
        ? System.getProperty(key)
        : AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(key));
  }

  /**
   * Test of addFile method, of class ClassPathHacker.
   */
  //@Test
  //public void testAddFile_String() throws Exception {
  //    String s = "test_item";
  //    ClasspathRegister.addPath(s);
  //    String classPath = System.getProperty("java.class.path");
  //    assertTrue(classPath.contains(s));
  //}

  //@Test
  //public void testAddFile() throws Exception {
  //    File f = new File(".");
  //    String s = f.getAbsolutePath();
  //    ClasspathRegister.addPath(f.toPath());
  //    String classPath = System.getProperty("java.class.path");
  //    assertTrue(classPath.contains(s));
  //}
}
