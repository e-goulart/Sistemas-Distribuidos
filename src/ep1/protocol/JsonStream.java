package ep1.protocol;

import java.io.IOException;
import java.io.Reader;

/** Lê exatamente um objeto JSON por conexão, com ou sem newline ao final. */
public final class JsonStream {
    private static final int MAX_CHARS = 65_536;
    private JsonStream() { }

    public static String read(Reader input) throws IOException {
        StringBuilder result = new StringBuilder();
        int level = 0;
        boolean started = false;
        boolean insideString = false;
        boolean escaped = false;
        for (int c; (c = input.read()) != -1;) {
            char current = (char) c;
            if (!started) {
                if (Character.isWhitespace(current)) continue;
                if (current != '{') throw new IOException("A requisição deve ser um objeto JSON");
                started = true;
            }
            result.append(current);
            if (result.length() > MAX_CHARS) throw new IOException("Mensagem maior que 64 KB");
            if (insideString) {
                if (escaped) escaped = false;
                else if (current == '\\') escaped = true;
                else if (current == '"') insideString = false;
            } else if (current == '"') insideString = true;
            else if (current == '{' || current == '[') level++;
            else if (current == '}' || current == ']') {
                level--;
                if (level == 0) return result.toString();
                if (level < 0) throw new IOException("JSON malformado");
            }
        }
        if (!started) throw new IOException("Conexão encerrada sem requisição");
        throw new IOException("Objeto JSON incompleto");
    }
}
