package com.cm.wifiscanner.util;

public class Logger {

    private static interface Printer {
        public void i(String tag, String msg);
        public void d(String tag, String msg);
        public void w(String tag, String msg);
        public void e(String tag, String msg);
    }

    private static Printer sPrinter;

    static {
        sPrinter = new Outputter();
    }

    public static void warning(String tag, String msg) {
        sPrinter.w(tag, msg);
    }

    public static void debug(String tag, String msg) {
        sPrinter.d(tag, msg);
    }

    public static void error(String tag, String msg) {
        sPrinter.e(tag, msg);
    }

    private static class Outputter implements Printer {

        @Override
        public void i(String tag, String msg) {
            System.out.println("[" + tag + "]: " + msg);
        }

        @Override
        public void d(String tag, String msg) {
            System.out.println("[" + tag + "]: " + msg);
        }

        @Override
        public void w(String tag, String msg) {
            System.out.println("[" + tag + "]: " + msg);
        }

        @Override
        public void e(String tag, String msg) {
            System.out.println("[" + tag + "]: " + msg);
        }
    }

}
