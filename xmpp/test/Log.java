package com.xetrix;

public class Log {
  private static final String             ESC_NORMAL = (char)27 + "[0m";
  private static final String             ESC_GREY = (char)27 + "[0m";
  private static final String             ESC_WHITE = (char)27 + "[01;37m";
  private static final String             ESC_RED = (char)27 + "[01;31m";
  private static final String             ESC_GREEN = (char)27 + "[01;32m";
  private static final String             ESC_YELLOW = (char)27 + "[01;33m";
  private static final String             ESC_BLUE = (char)27 + "[01;34m";
  private static final String             ESC_BLACK = (char)27 + "[01;30m";

  private static Long                     timeZero = System.currentTimeMillis();

  public static void write(String m, Integer l) {
    String prefix = buildPrefix(l);
    String suffix = buildSuffix(l);
    System.out.println(prefix + m + suffix);
  }

  public static void write(String m) {
    write(m, 6);
  }

  private static String buildPrefix(Integer l) {
    Long   time = (System.currentTimeMillis() - timeZero);
    String c = colorByLevel(l);
    String r = ESC_NORMAL;
    String t = time.toString();
    String s = "          ".substring(10 + (t.length()-10));

    switch (l) {
      case 0:
        return c + "ECY" + r + " [" + t + "] " + s;
      case 1:
        return c + "ALT" + r + " [" + t + "] " + s;
      case 2:
        return c + "CRT" + r + " [" + t + "] " + s;
      case 3:
        return c + "ERR" + r + " [" + t + "] " + s;
      case 4:
        return c + "DGR" + r + " [" + t + "] " + s;
      case 5:
        return c + "WRN" + r + " [" + t + "] " + s;
      case 6:
        return c + "INF" + r + " [" + t + "] " + s;
      case 7:
        return c + "DBG" + r + " [" + t + "] " + s;
    }
    return c + ">>>" + r + " [" + t + "] " + s;
  }

  private static String buildSuffix(Integer l) {
    return "";
  }

  private static String colorByLevel(Integer l) {
    if (l<4) {
      return ESC_RED;
    } else if (l<6) {
      return ESC_YELLOW;
    } else if (l<7) {
      return ESC_GREEN;
    } else if (l<8) {
      return ESC_WHITE;
    } else {
      return ESC_NORMAL;
    }
  }
}