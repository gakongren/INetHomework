package cn.sorato.inet.homework1;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

class ThreadedEchoHandler implements Runnable {
    private Socket incoming;

    private BufferedReader in = null;
    private BufferedOutputStream out = null;

    ThreadedEchoHandler(Socket i) {
        incoming = i;
    }

    private void log(String line) {
        System.out.println("INFO: " + line);
    }

    private File parseFilePath() throws IOException {
        String line;
        File path = null;
        while (in != null && !(line = in.readLine()).equals("")) {
            log(line);
            if (line.startsWith("GET")) {
                String pathstr = "." + line.split(" ")[1];
                pathstr = URLDecoder.decode(pathstr, "UTF-8");
                path = new File(pathstr);
            }
        }
        return path;
    }

    String[] getFileList(File file) {
        List<File> list = new ArrayList<>();
        if (!file.getPath().equals("."))
            list.add(new File(".."));
        if (file.listFiles() != null)
            Collections.addAll(list, file.listFiles());
        return list.stream()
                .map(f -> "<a href=\"" + f.getPath() + "\">" + f.getName() + "</a>")
                .toArray(String[]::new);
    }

    private void outputFile(File path) throws IOException {
        if (path == null)
            return;
        if (!path.exists()) {
            out.write(assembleHtmlBody(
                    return404(),
                    path.getName(),
                    "<b>404 Not Found</b>",
                    "Check your file name."
            ).getBytes());
            out.flush();
        } else {
            if (path.isFile()) {
                BufferedInputStream fin = new BufferedInputStream(new FileInputStream(path));
                byte bytes[] = new byte[1024];
                out.flush();
                out.write(return200().getBytes());
                while (fin.available() > 0) {
                    int size = fin.read(bytes);
                    out.write(bytes, 0, size);
                }
                out.flush();
            } else if (path.isDirectory()) {
                out.write(assembleHtmlBody(
                        return200(),
                        path.getName(),
                        getFileList(path)
                ).getBytes());
                out.flush();
            }
        }
        in.close();
        out.close();
    }

    private String assembleHtmlBody(String header, String title, String... context) {
        Optional<String> ctx = Stream.of(context).reduce((a, b) -> a + "<br/>" + b);
        return header + "<html><title>" + title + "</title>" + (ctx.isPresent() ? ctx.get() : "") + "</html>";
    }

    public void run() {
        try {
            InputStream inStream = incoming.getInputStream();
            OutputStream outStream = incoming.getOutputStream();
            in = new BufferedReader(new InputStreamReader(inStream));
            out = new BufferedOutputStream(outStream);
        } catch (IOException e) {
            e.printStackTrace();
            try {
                incoming.close();
            } catch (IOException ignored) {
            }
            return;
        }

        File path = null;
        try {
            path = parseFilePath();
            log("Parse request finished.");
            outputFile(path);
            incoming.close();
        } catch (IOException e) {
            try {
                out.write(assembleHtmlBody(
                        return500(),
                        "Error",
                        "<b>500 Internal Server Error</b>",
                        e.getMessage()
                ).getBytes());
                out.flush();
                out.close();
            } catch (IOException ignored) {
            }
        }

    }

    private String return500() {
        return "HTTP/1.1 500 Internal Server Error\n" +
                "Connection: close\n\n";
    }

    private String return200() {
        return "HTTP/1.1 200 OK\r\n" +
                "Connection: close\r\n" +
                "Server: RAW-Java server\r\n" +
                "\r\n";
    }

    private String return404() {
        return "HTTP/1.1 404 Not Found\n" +
                "Connection: close\n\n";
    }
}

public class ThreadEchoServer {
    public static void main(String[] args) {
        ServerSocket s;
        try {
            s = new ServerSocket(18888);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Socket incoming = null;
        while (true) {
            try {
                incoming = s.accept();
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
            Runnable r = new ThreadedEchoHandler(incoming);
            new Thread(r).start();
        }
    }
}
