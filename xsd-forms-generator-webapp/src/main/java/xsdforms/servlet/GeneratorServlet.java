package xsdforms.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import scala.Option;
import xsdforms.Generator;

public class GeneratorServlet extends HttpServlet {

	private static final long serialVersionUID = -1329200353122439077L;
	private static final String PARAMETER_EXTRA_SCRIPT = "extraScript";
	private static final String PARAMETER_ROOT_ELEMENT = "rootElement";
	private static final String PARAMETER_ID_PREFIX = "idPrefix";
	private static final String ACTION_ZIP = "zip";

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String schema = req.getParameter("schema");
		InputStream schemaIn = new ByteArrayInputStream(schema.getBytes());
		String idPrefix = nullToBlank(req.getParameter(PARAMETER_ID_PREFIX));
		String rootElementParam = blankToNull(req.getParameter(PARAMETER_ROOT_ELEMENT));
		Option<String> rootElement = Option.apply(rootElementParam);
		String extraScriptParameter = blankToNull(req
				.getParameter(PARAMETER_EXTRA_SCRIPT));
		Option<String> extraScript = Option.apply(extraScriptParameter);

		String action = req.getParameter("action");
		if (ACTION_ZIP.equals(action)) {
			resp.setContentType("application/zip");
			resp.setHeader("Content-Disposition",
					"attachment; filename=site.zip");
			Generator.generateZip(schemaIn, resp.getOutputStream(), idPrefix,
					rootElement, extraScript);
		} else {
			resp.setContentType("text/html");
			Generator.generateHtml(schemaIn, resp.getOutputStream(), idPrefix,
					rootElement, extraScript);
		}
	}

	private static String blankToNull(String s) {
		if (s != null && s.trim().length() == 0)
			return null;
		else
			return s;
	}

	private static String nullToBlank(String s) {
		if (s == null)
			return "";
		else
			return s;
	}

}
