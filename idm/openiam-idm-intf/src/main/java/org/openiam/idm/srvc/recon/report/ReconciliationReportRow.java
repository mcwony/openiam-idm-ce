package org.openiam.idm.srvc.recon.report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class ReconciliationReportRow {
	private static final String HEADER_COLOR = "#a3a3a3";
	private static final String SUB_HEADER_COLOR = "#c3c3c3";
	private static final String CONFLICT_COLOR = "#ff4455";
	/**
	 * 
	 */

	private String htmlRow = "";
	private String csvRow = "";

	public ReconciliationReportRow(String header) throws Exception {
		super();
		htmlRow = generateHTMLHeader(header);
		csvRow = generateCSVHeader(header);
	}

	private String generateHTMLHeader(String header) throws Exception {
		StringBuilder build = new StringBuilder();

		if (StringUtils.isEmpty(header) || header.split(",").length < 1) {
			throw new Exception("wrongHeader");
		}
		String head[] = header.split(",");
		build.append("<tr style='background-color:" + HEADER_COLOR + "'>");
		build.append("<td rowSpan='2' align='center'>");
		build.append("Case");
		build.append("</td>");
		build.append("<td align='center' colSpan='" + head.length + "'>");
		build.append("Fields name");
		build.append("</td>");
		build.append("</tr>");
		build.append("<tr style='background-color:" + HEADER_COLOR + "'>");
		for (String str : head) {
			build.append("<td>");
			build.append(str);
			build.append("</td>");
		}
		build.append("</tr>");
		return build.toString();
	}

	private String generateCSVHeader(String header) throws Exception {
		StringBuilder build = new StringBuilder();
		build.append("\"CASE\",");
		build.append("\"");
		build.append(header.replace(",", "\",\""));
		build.append("\"");
		build.append('\n');
		return build.toString();
	}

	public ReconciliationReportRow(String sepatatorText, int colSpan)
			throws Exception {
		StringBuilder build = new StringBuilder();
		build.append("<tr style='background-color:" + SUB_HEADER_COLOR + "'>");
		build.append("<td colSpan='" + colSpan + "' align='center'>");
		build.append(sepatatorText);
		build.append("</td>");
		htmlRow = build.toString();
	}

	public ReconciliationReportRow(String preffix,
			ReconciliationReportResults result, String values) throws Exception {
		super();
		htmlRow = generateHTMLRow(preffix, result, values);
		csvRow = generateCSVRow(preffix, result, values);
	}

	private String generateCSVRow(String preffix,
			ReconciliationReportResults result, String values) throws Exception {
		StringBuilder build = new StringBuilder();
		build.append("\"");
		build.append(preffix);
		build.append(result.getValue());
		build.append("\"");
		build.append(',');
		build.append("\"");
		build.append(values.replace(",", "\",\""));
		build.append("\"");
		build.append('\n');
		return build.toString();
	}

	private String generateHTMLRow(String preffix,
			ReconciliationReportResults result, String values) throws Exception {
		StringBuilder build = new StringBuilder();
		List<String> vals = new ArrayList<String>(Arrays.asList(values
				.split(",")));
		if (result == null || StringUtils.isEmpty(values)
				|| values.split(",").length < 1) {
			throw new Exception("wrong data");
		}
		// Check diffs
		StringBuilder td = new StringBuilder();
		for (String str : vals) {
			if (str.contains("][")) {
				result = ReconciliationReportResults.MATCH_FOUND_DIFFERENT;
				td.append("<td style='background-color:" + CONFLICT_COLOR
						+ "'>");
			} else
				td.append("<td>");
			td.append(str);
			td.append("</td>");
		}

		if (values.charAt(values.length() - 1) == ',')
			vals.add("&nbsp;");
		build.append("<tr>");
		build.append("<td style='width:200px;background-color:"
				+ result.getColor() + ";'>");
		build.append(preffix);
		build.append(result.getValue());
		build.append("</td>");
		build.append(td);
		build.append("</tr>");
		return build.toString();
	}

	public String toHTML() {
		return htmlRow;
	}

	public String toCSV() {
		return csvRow;
	}
}