/*
 * TGC
 *
 * Class Utils
 *
 * (c) thSoft
 */

package hu.thsoft;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.net.*;

/**
 * Some static multi-purpose utility functions.
 *
 * @author thSoft
 */
public class Utils {
  
  /**
   * Converts an IP address to a more user-friendly unique string id.
   */
  public static String ip2id(InetAddress ip) {
    final char table[] = {'a', 'e', 'i', 'o', 'u', 'A', 'E', 'I', 'O', 'U'};
    byte bytes[] = ip.getAddress();
    int a;
    String id = "";
    for (int i = 0; i < 4; i++) {
      a = bytes[i] & 0xff;
      id += table[a/26];
      id += (char)('a'+a%26);
    }
    return id;
  }
  
  /**
   * Converts a unique string id returned by <code>ip2id</code> back to
   * an IP address. Returns <code>null</code> if the given id is not valid.
   */
  public static InetAddress id2ip(String id) {
    final String table = "aeiouAEIOU";
    byte bytes[] = {0, 0, 0, 0};
    int p = 0;
    if (id.length() != 8) {
      return null;
    }
    for (int i = 0; i < 4; i++) {
      bytes[i] =
        (byte)(table.indexOf(id.charAt(p++))*26+(int)(id.charAt(p++)-'a'));
    }
    try {
      return (InetAddress.getByAddress(bytes));
    } catch (UnknownHostException e) {
      return null;
    }
  }
  
  /**
   * Gets the <code>String</code> content of the system clipboard.
   */
  public static String getClipboardString() {
    Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().
      getContents(null);    
    try {
      if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
        String text = (String)t.getTransferData(DataFlavor.stringFlavor);
        return text;
      }
    } catch (UnsupportedFlavorException e) {
    } catch (IOException e) {
    }
    return null;
  }
  
  /**
   * Writes a <code>String</code> to the system clipboard.
   */
  public static void setClipboardString(String str) {
    StringSelection ss = new StringSelection(str);
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
  }

  /**
   * Returns a string similar to <code>s</code> except that its first
   * character will be upper case.
   */
  public static String firstUpcase(String s) {
    if (s.equals("")) {
      return "";
    } else {
      return new String(Character.toUpperCase(s.charAt(0))+s.substring(1));
    }
  }
  
}
