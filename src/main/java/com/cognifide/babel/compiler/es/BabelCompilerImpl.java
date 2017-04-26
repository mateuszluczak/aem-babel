package com.cognifide.babel.compiler.es;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.ui.clientlibs.script.CompilerContext;
import com.adobe.granite.ui.clientlibs.script.ScriptCompiler;
import com.adobe.granite.ui.clientlibs.script.ScriptResource;
import jdk.nashorn.api.scripting.NashornScriptEngine;

@Component
@Service(ScriptCompiler.class)
public class BabelCompilerImpl implements ScriptCompiler {

    private static final Logger log = LoggerFactory.getLogger(BabelCompilerImpl.class);
    private static final String JS_MIME_TYPE = "application/javascript";
    private static final String BABEL_EXTENSION = "es6";
    private static final String JS_EXTENSION = "js";

    private final NashornScriptEngine js;

    public BabelCompilerImpl() throws IOException, ScriptException {
        long t0 = System.currentTimeMillis();
        this.js = (NashornScriptEngine) new ScriptEngineManager().getEngineByName("nashorn");

        final Bindings bindings = js.getBindings(ScriptContext.ENGINE_SCOPE);

        bindings.put("babel", this);
        bindings.put("babel_repackArgs", js.eval("(function () { return arguments; })"));

        loadLocalLib("babel.js");

        long t1 = System.currentTimeMillis();
        log.info("Initialized Babel Compiler in {}ms", t1 - t0);
    }

    public void compile(Collection<ScriptResource> src, Writer dst, CompilerContext ctx) throws IOException {
        long t0 = System.currentTimeMillis();

        final Bindings bindings = js.getBindings(ScriptContext.ENGINE_SCOPE);
        final Object babel = bindings.get("babel");
        SimpleBindings bindings = new SimpleBindings();

        for (ScriptResource r : src) {
            Reader inputReader = r.getReader();
            String inputStream = IOUtils.toString(inputReader));
            inputReader.close();
            bindings.put("input", inputStream);

            try {
                Object output = engine.eval("Babel.transform(input, { presets: ['es2015'] }).code", bindings);
            } catch (ScriptException | NoSuchMethodException e) {
                log.error(e.getMessage());
            }

            IOUtils.write(output.toString(), dst);
        }
        dst.close();
        long t1 = System.currentTimeMillis();
        log.info("Compile Babel in {}ms", t1 - t0);
    }

    private void loadLocalLib(final String filename) throws IOException, ScriptException {
        try (final InputStream in = getClass().getResourceAsStream(filename)) {
            final InputStreamReader reader = new InputStreamReader(in);
            js.eval(reader);
        }
    }

    public boolean handles(String extension) {
        return StringUtils.equals(extension, BABEL_EXTENSION) || StringUtils.equals(extension, "." + BABEL_EXTENSION);
    }

    public String getName() {
        return BABEL_EXTENSION;
    }

    public String getMimeType() {
        return JS_MIME_TYPE;
    }

    public String getOutputExtension() {
        return JS_EXTENSION;
    }
}