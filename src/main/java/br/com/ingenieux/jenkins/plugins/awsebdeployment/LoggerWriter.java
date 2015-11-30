package br.com.ingenieux.jenkins.plugins.awsebdeployment;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class LoggerWriter extends PrintWriter {
    private final StringWriter result;

    private LoggerWriter(StringWriter result) {
        super(result);
        this.result = result;
    }

    public void log(String message, Object... args) {
        println(String.format(message, args));
    }

    public String getResult() {
        return result.toString();
    }

    public static LoggerWriter get() {
        return new LoggerWriter(new StringWriter());
    }
}
