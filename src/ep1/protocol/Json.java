package ep1.protocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pequeno codificador/decodificador JSON: sem bibliotecas externas. */
public final class Json {
    private Json() { }

    public static Map<String, Object> object(Object... entries) {
        if (entries.length % 2 != 0) throw new IllegalArgumentException("pares de chave/valor");
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            result.put((String) entries[i], entries[i + 1]);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asObject(Object item) {
        if (!(item instanceof Map<?, ?>)) throw new IllegalArgumentException("JSON deve ser um objeto");
        Map<?, ?> map = (Map<?, ?>) item;
        for (Object key : map.keySet()) if (!(key instanceof String)) throw new IllegalArgumentException("Chave inválida");
        return (Map<String, Object>) map;
    }

    public static String field(Map<String, Object> object, String key) {
        Object value = object.get(key);
        return value instanceof String ? (String) value : null;
    }

    public static Object parse(String text) {
        Parser parser = new Parser(text);
        Object value = parser.value();
        parser.spaces();
        if (parser.pos < text.length()) throw new IllegalArgumentException("JSON contém caracteres extras");
        return value;
    }

    public static String stringify(Object item) {
        StringBuilder result = new StringBuilder();
        write(result, item);
        return result.toString();
    }

    private static void write(StringBuilder out, Object item) {
        if (item == null) out.append("null");
        else if (item instanceof String) {
            out.append('"');
            for (char ch : ((String) item).toCharArray()) {
                switch (ch) {
                    case '"': out.append("\\\""); break;
                    case '\\': out.append("\\\\"); break;
                    case '\n': out.append("\\n"); break;
                    case '\r': out.append("\\r"); break;
                    case '\t': out.append("\\t"); break;
                    default:
                        if (ch < 32) out.append(String.format("\\u%04x", (int) ch));
                        else out.append(ch);
                }
            }
            out.append('"');
        } else if (item instanceof Boolean || item instanceof Number) out.append(item);
        else if (item instanceof Map<?, ?>) {
            out.append('{');
            boolean comma = false;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) item).entrySet()) {
                if (!(entry.getKey() instanceof String)) throw new IllegalArgumentException("Chave não textual");
                if (comma) out.append(',');
                write(out, entry.getKey());
                out.append(':');
                write(out, entry.getValue());
                comma = true;
            }
            out.append('}');
        } else if (item instanceof List<?>) {
            out.append('[');
            boolean comma = false;
            for (Object value : (List<?>) item) {
                if (comma) out.append(',');
                write(out, value);
                comma = true;
            }
            out.append(']');
        } else throw new IllegalArgumentException("Tipo JSON não suportado: " + item.getClass());
    }

    private static final class Parser {
        private final String text;
        private int pos;
        Parser(String input) { text = input; }
        void spaces() {
            while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) pos++;
        }
        boolean take(char ch) {
            spaces();
            if (pos < text.length() && text.charAt(pos) == ch) { pos++; return true; }
            return false;
        }
        void require(char ch) {
            if (!take(ch)) throw new IllegalArgumentException("Esperado '" + ch + "' na posição " + pos);
        }
        Object value() {
            spaces();
            if (pos >= text.length()) throw new IllegalArgumentException("JSON incompleto");
            char ch = text.charAt(pos);
            if (ch == '{') return map();
            if (ch == '[') return array();
            if (ch == '"') return string();
            if (ch == 't' && keyword("true")) return true;
            if (ch == 'f' && keyword("false")) return false;
            if (ch == 'n' && keyword("null")) return null;
            if (ch == '-' || Character.isDigit(ch)) return number();
            throw new IllegalArgumentException("Valor JSON inválido na posição " + pos);
        }
        boolean keyword(String key) {
            if (text.startsWith(key, pos)) { pos += key.length(); return true; }
            return false;
        }
        Map<String, Object> map() {
            require('{');
            Map<String, Object> data = new LinkedHashMap<>();
            if (take('}')) return data;
            do {
                spaces();
                if (pos >= text.length() || text.charAt(pos) != '"') throw new IllegalArgumentException("Chave JSON inválida");
                String key = string();
                require(':');
                data.put(key, value());
                if (take('}')) return data;
                require(',');
            } while (true);
        }
        List<Object> array() {
            require('[');
            List<Object> elements = new ArrayList<>();
            if (take(']')) return elements;
            do {
                elements.add(value());
                if (take(']')) return elements;
                require(',');
            } while (true);
        }
        String string() {
            require('"');
            StringBuilder out = new StringBuilder();
            while (pos < text.length()) {
                char ch = text.charAt(pos++);
                if (ch == '"') return out.toString();
                if (ch == '\\') {
                    if (pos >= text.length()) throw new IllegalArgumentException("Escape incompleto");
                    char escape = text.charAt(pos++);
                    switch (escape) {
                        case '"': out.append('"'); break;
                        case '\\': out.append('\\'); break;
                        case '/': out.append('/'); break;
                        case 'b': out.append('\b'); break;
                        case 'f': out.append('\f'); break;
                        case 'n': out.append('\n'); break;
                        case 'r': out.append('\r'); break;
                        case 't': out.append('\t'); break;
                        case 'u':
                            if (pos + 4 > text.length()) throw new IllegalArgumentException("Unicode incompleto");
                            try { out.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16)); }
                            catch (NumberFormatException e) { throw new IllegalArgumentException("Unicode inválido"); }
                            pos += 4;
                            break;
                        default: throw new IllegalArgumentException("Escape inválido");
                    }
                } else {
                    if (ch < 32) throw new IllegalArgumentException("Caractere de controle em string");
                    out.append(ch);
                }
            }
            throw new IllegalArgumentException("String JSON sem fechamento");
        }
        Number number() {
            int start = pos;
            if (text.charAt(pos) == '-') pos++;
            while (pos < text.length() && Character.isDigit(text.charAt(pos))) pos++;
            if (pos < text.length() && text.charAt(pos) == '.') {
                pos++;
                while (pos < text.length() && Character.isDigit(text.charAt(pos))) pos++;
            }
            if (pos < text.length() && (text.charAt(pos) == 'e' || text.charAt(pos) == 'E')) {
                pos++;
                if (pos < text.length() && (text.charAt(pos) == '+' || text.charAt(pos) == '-')) pos++;
                while (pos < text.length() && Character.isDigit(text.charAt(pos))) pos++;
            }
            try {
                String number = text.substring(start, pos);
                if (number.contains(".") || number.contains("e") || number.contains("E")) return Double.parseDouble(number);
                return Long.parseLong(number);
            } catch (NumberFormatException e) { throw new IllegalArgumentException("Número JSON inválido"); }
        }
    }
}
