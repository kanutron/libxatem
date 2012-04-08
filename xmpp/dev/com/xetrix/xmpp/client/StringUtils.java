package com.xetrix.xmpp.client;

public class StringUtils {
  private static final char[] QUOTE_ENCODE = "&quot;".toCharArray();
  private static final char[] APOS_ENCODE = "&apos;".toCharArray();
  private static final char[] AMP_ENCODE = "&amp;".toCharArray();
  private static final char[] LT_ENCODE = "&lt;".toCharArray();
  private static final char[] GT_ENCODE = "&gt;".toCharArray();

  public static String escapeForXML(String string) {
    if (string == null) {
      return null;
    }
    char ch;
    int i=0;
    int last=0;
    char[] input = string.toCharArray();
    int len = input.length;
    StringBuilder out = new StringBuilder((int)(len*1.3));
    for (; i < len; i++) {
      ch = input[i];
      if (ch > '>') {
      } else if (ch == '<') {
        if (i > last) {
          out.append(input, last, i - last);
        }
        last = i + 1;
        out.append(LT_ENCODE);
      } else if (ch == '>') {
        if (i > last) {
          out.append(input, last, i - last);
        }
        last = i + 1;
        out.append(GT_ENCODE);
      } else if (ch == '&') {
        if (i > last) {
          out.append(input, last, i - last);
        }
        // Do nothing if the string is of the form &#235; (unicode value)
        if (!(len > i + 5
            && input[i + 1] == '#'
            && Character.isDigit(input[i + 2])
            && Character.isDigit(input[i + 3])
            && Character.isDigit(input[i + 4])
            && input[i + 5] == ';')) {
          last = i + 1;
          out.append(AMP_ENCODE);
        }
      } else if (ch == '"') {
        if (i > last) {
          out.append(input, last, i - last);
        }
        last = i + 1;
        out.append(QUOTE_ENCODE);
      } else if (ch == '\'') {
        if (i > last) {
          out.append(input, last, i - last);
        }
        last = i + 1;
        out.append(APOS_ENCODE);
      }
    }
    if (last == 0) {
      return string;
    }
    if (i > last) {
      out.append(input, last, i - last);
    }
    return out.toString();
  }
}
