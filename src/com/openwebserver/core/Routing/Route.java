package com.openwebserver.core.Routing;


import com.openwebserver.core.Domain;
import com.openwebserver.core.Objects.Request;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Route {

    public enum Method {
        GET,
        HEAD,
        POST,
        PUT,
        DELETE,
        CONNECT,
        OPTIONS,
        TRACE,
        PATCH,
        UNDEFINED;

        public boolean allows(Method method) {
            return method == this || this == Method.UNDEFINED;
        }
    }

    private final String[] require;
    private String path;
    private final Route.Method method;
    private final HashMap<Integer, String> RESTParams = new HashMap<>();
    private Domain domain;
    private boolean needsAuthorization = false;

    public Route(String path, Method method, String ... require){
        this.path = path;
        this.method = method;
        this.require = require;
        if(path != null) {
            if (!path.startsWith("/") && !path.equals("#")) path = "/" + path;
            RESTDecoder.PatternReader(path, RESTParams::put);
        }
    }

    public Route(com.openwebserver.services.Annotations.Route route){
        this(route.path(), route.method(), route.require());
    }

    public Route(Route route){
        this(route.getPath(), route.getMethod(), route.getRequired());
        this.domain = route.domain;
    }

    public boolean needsAuthentication() {
        return needsAuthorization;
    }

    public void setNeedsAuthentication(boolean authorized) {
        this.needsAuthorization = authorized;
    }

    protected Method getMethod() {
        return method;
    }

    public String[] getRequired() {
        return require;
    }

    protected boolean hasRequired(Request request) {
        return request.GET().keySet().containsAll(Arrays.asList(getRequired())) || request.POST().keySet().containsAll(Arrays.asList(getRequired()));
    }

    public String getPath() {
        return path;
    }

    public void addPrefix(Route notation){
        addPrefix(notation.getPath());
    }

    public void addPrefix(String prefix) {
        String finalPrefix = prefix.contains("//")?prefix.replaceAll("//", ""): prefix;
        if(!this.path.contains(prefix)) this.path = finalPrefix + getPath();
        if(path.contains("//")) path = path.replaceAll("//", "/");
        if(isREST()){
            RESTParams.clear();
            RESTDecoder.PatternReader(path, RESTParams::put);
        }
    }

    private boolean isREST() {
        return !RESTParams.isEmpty();
    }

    public void setDomain(Domain domain) {
        this.domain = domain;
    }

    public Domain getDomain() {
        return domain;
    }

    public static class RESTDecoder {

        public final static Pattern pattern = Pattern.compile("\\{(.*?)}", Pattern.MULTILINE);

        public static boolean containsRegex(String path){
            return path.contains("{") && path.contains("}");
        }

        static boolean Match(String request, Route route, BiConsumer<String, String> paramConsumer) {
            String[] requestPath = request.split("/");
            String[] handlerPath = route.getPath().split("/");
            if (requestPath.length != handlerPath.length) {
                return false;
            }
            for (int i = 0; i < requestPath.length; i++) {
                if (route.RESTParams.containsKey(i)) {
                    paramConsumer.accept(route.RESTParams.get(i).replace("{", "").replace("}", ""), requestPath[i]);
                    continue;
                }
                if (!requestPath[i].equals(handlerPath[i])) {
                    return false;
                }
            }
            return true;
        }

        private static void PatternReader(String value, BiConsumer<Integer, String> matchConsumer) {
            Matcher matcher = pattern.matcher(value);
            ArrayList<String> pathIndexed = new ArrayList<>(Arrays.asList(value.split("/")));
            while (matcher.find()) {
                String match = matcher.group(0);
                matchConsumer.accept(pathIndexed.indexOf(match), match);
            }
        }

    }



}
