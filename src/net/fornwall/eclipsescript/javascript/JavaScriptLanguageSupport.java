package net.fornwall.eclipsescript.javascript;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import net.fornwall.eclipsescript.javascript.CustomContextFactory.CustomContext;
import net.fornwall.eclipsescript.javascript.JavascriptRuntime.DieError;
import net.fornwall.eclipsescript.javascript.JavascriptRuntime.ExitError;
import net.fornwall.eclipsescript.scriptobjects.Eclipse;
import net.fornwall.eclipsescript.scripts.IScriptLanguageSupport;
import net.fornwall.eclipsescript.scripts.ScriptAbortException;
import net.fornwall.eclipsescript.scripts.ScriptException;
import net.fornwall.eclipsescript.scripts.ScriptMetadata;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextAction;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrappedException;

public class JavaScriptLanguageSupport implements IScriptLanguageSupport {

	private final CustomContextFactory contextFactory = new CustomContextFactory();

	@Override
	public void executeScript(final ScriptMetadata script) {
		contextFactory.call(new ContextAction() {

			@Override
			public Object run(Context _context) {
				if (!(_context instanceof CustomContext))
					throw new IllegalArgumentException("Wrong context class: " + _context.getClass()); //$NON-NLS-1$
				CustomContext context = (CustomContext) _context;
				ScriptableObject scope = new ImporterTopLevel(context);
				JavascriptRuntime jsRuntime = new JavascriptRuntime(context, scope, script.getFile());

				Eclipse eclipseJavaObject = new Eclipse(jsRuntime);
				Object eclipseJsObject = Context.javaToJS(eclipseJavaObject, scope);
				ScriptableObject.putConstProperty(scope, Eclipse.VARIABLE_NAME, eclipseJsObject);

				Reader reader = null;
				try {
					reader = new InputStreamReader(script.getFile().getContents(), script.getFile().getCharset());
					jsRuntime.evaluate(reader, script.getFile().getName());
				} catch (ExitError e) {
					// do nothing
				} catch (DieError e) {
					throw new ScriptAbortException(e.getMessage(), e.evalException, e.evalException.lineNumber());
				} catch (RhinoException e) {
					boolean showStackTrace = (e instanceof WrappedException);
					Throwable cause = showStackTrace ? ((WrappedException) e).getCause() : e;
					throw new ScriptException(e.getMessage(), cause, e.lineNumber(), showStackTrace);
				} catch (Exception e) {
					throw (e instanceof RuntimeException) ? ((RuntimeException) e) : new RuntimeException(e);
				} finally {
					try {
						if (reader != null)
							reader.close();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
				return null;
			}
		});
	}
}