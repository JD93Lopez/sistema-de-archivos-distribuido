package backend_cliente;

import java.util.*;

public class JsonParser {

    public static Map<String, Object> parse(String json) {
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            throw new IllegalArgumentException("JSON debe ser un objeto");
        }
        json = json.substring(1, json.length() - 1).trim();
        return parseObject(json);
    }

    private static Map<String, Object> parseObject(String json) {
        Map<String, Object> obj = new LinkedHashMap<>();
        int i = 0;
        while (i < json.length()) {
            if (json.charAt(i) == ' ' || json.charAt(i) == '\t' || json.charAt(i) == '\n') {
                i++;
                continue;
            }
            if (json.charAt(i) == ',') {
                i++;
                continue;
            }

            if (json.charAt(i) != '"') {
                throw new IllegalArgumentException("Clave debe estar entre comillas");
            }
            int endKey = json.indexOf('"', i + 1);
            if (endKey == -1) throw new IllegalArgumentException("Clave sin cierre");
            String key = json.substring(i + 1, endKey);
            i = endKey + 1;

            while (i < json.length() && json.charAt(i) != ':') i++;
            if (i >= json.length()) throw new IllegalArgumentException("Falta :");
            i++;

            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;

            Object value;
            if (json.charAt(i) == '"') {
                int end = i + 1;
                boolean escape = false;
                while (end < json.length()) {
                    char c = json.charAt(end);
                    if (c == '\\' && !escape) {
                        escape = true;
                    } else if (c == '"' && !escape) {
                        break;
                    } else {
                        escape = false;
                    }
                    end++;
                }
                if (end >= json.length()) throw new IllegalArgumentException("String sin cierre");
                value = json.substring(i + 1, end);
                i = end + 1;
            } else if (json.charAt(i) == '{') {
                int depth = 1;
                int start = i;
                i++;
                while (i < json.length() && depth > 0) {
                    if (json.charAt(i) == '{') depth++;
                    else if (json.charAt(i) == '}') depth--;
                    i++;
                }
                if (depth != 0) throw new IllegalArgumentException("Objeto sin cerrar");
                value = parseObject(json.substring(start + 1, i - 1));
            } else if (Character.isDigit(json.charAt(i)) || json.charAt(i) == '-') {
                int start = i;
                while (i < json.length() && (Character.isDigit(json.charAt(i)) || json.charAt(i) == '.')) i++;
                value = Long.parseLong(json.substring(start, i));
            } else if (json.startsWith("true", i)) {
                value = true;
                i += 4;
            } else if (json.startsWith("false", i)) {
                value = false;
                i += 5;
            } else if (json.startsWith("null", i)) {
                value = null;
                i += 4;
            } else {
                throw new IllegalArgumentException("Valor no reconocido en posición " + i);
            }

            obj.put(key, value);
        }
        return obj;
    }

    public static String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) {
            String s = (String) obj;
            return "\"" + s.replace("\"", "\\\"").replace("\\", "\\\\") + "\"";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":").append(toJson(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof NodoArbol) {
            NodoArbol nodo = (NodoArbol) obj;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("nombre", nodo.getNombre());
            m.put("tamano", nodo.getTamano());
            m.put("hijos", nodo.getHijos());
            return toJson(m);
        }
        if (obj instanceof ArbolEspacio) {
            ArbolEspacio arbol = (ArbolEspacio) obj;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("espacioTotal", arbol.getEspacioTotal());
            m.put("espacioUsado", arbol.getEspacioUsado());
            m.put("raiz", arbol.getRaiz());
            return toJson(m);
        }
        if (obj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) obj;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : list) {
                if (!first) sb.append(",");
                sb.append(toJson(item));
                first = false;
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"[object]\"";
    }
}