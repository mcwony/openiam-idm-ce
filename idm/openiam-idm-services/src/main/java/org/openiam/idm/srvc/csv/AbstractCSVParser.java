package org.openiam.idm.srvc.csv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openiam.idm.srvc.csv.constant.CSVSource;
import org.openiam.idm.srvc.csv.constant.UserFields;
import org.openiam.idm.srvc.mngsys.dto.AttributeMap;
import org.openiam.idm.srvc.mngsys.dto.ManagedSys;
import org.springframework.util.StringUtils;

public abstract class AbstractCSVParser<T, E extends Enum<E>> {
	protected static char SEPARATOR = ',';
	protected static char END_OF_LINE = '\n';
	protected static String PRINCIPAL_OBJECT = "PRINCIPAL";
	private String pathToCSV;

	/**
	 * @param pathToCSV
	 *            the pathToCSV to set
	 */
	public void setPathToCSV(String pathToCSV) {
		this.pathToCSV = pathToCSV;
	}

	protected static final Log log = LogFactory.getLog(AbstractCSVParser.class);

	private ReconciliationObject<T> csvToObject(Class<T> clazz,
			Class<E> clazz2, List<AttributeMap> attrMap,
			Map<String, String> object) throws InstantiationException,
			IllegalAccessException {
		ReconciliationObject<T> csvObject = new ReconciliationObject<T>();
		T obj = clazz.newInstance();
		for (AttributeMap a : attrMap) {
			String objValue = object.get(a.getAttributeName());
			if (StringUtils.hasText(objValue)) {
				if (a.getAttributePolicy() != null
						&& a.getAttributePolicy().getName() != null) {
					E fieldValue;
					try {
						fieldValue = Enum.valueOf(clazz2, a
								.getAttributePolicy().getName());
					} catch (IllegalArgumentException e) {
						log.info(e.getMessage());
						fieldValue = Enum.valueOf(clazz2, "DEFAULT");
					}
					this.putValueInDTO(obj, fieldValue, objValue.trim());
					if (PRINCIPAL_OBJECT.equals(a.getMapForObjectType())) {
						csvObject.setPrincipal(objValue);
					}
				}
			}
		}
		csvObject.setObject(obj);
		return csvObject;
	}

	protected abstract void putValueInDTO(T obj, E field, String value);

	protected abstract String putValueIntoString(T obj, E field);

	@SuppressWarnings("resource")
	private FileReader getCSVFile(ManagedSys mngSys,
			List<AttributeMap> attrMapList, Class<E> clazz, CSVSource source)
			throws Exception {
		if (attrMapList == null || attrMapList.isEmpty())
			return null;
		File file = new File(getFileName(mngSys, source));
		if (!file.exists()) {
			file.createNewFile();
			FileWriter writer = new FileWriter(file);
			writer.write(generateHeader(attrMapList, clazz));
			writer.flush();
			writer.close();
		}
		FileReader fr = new FileReader(file);
		BufferedReader br = new BufferedReader(fr);
		if (!StringUtils.hasText(br.readLine())) {
			FileWriter writer = new FileWriter(file);
			writer.write(generateHeader(attrMapList, clazz));
			writer.flush();
			writer.close();
		}
		return new FileReader(file);
	}

	/**
	 * when create csv
	 * 
	 * @param attrMap
	 * @return
	 */
	private String generateHeader(List<AttributeMap> attrMap, Class<E> clazz) {
		if (attrMap == null)
			return "";
		StringBuilder sb = new StringBuilder();
		for (AttributeMap a : attrMap) {
			if (StringUtils.hasText(a.getAttributeName())) {
				sb.append(a.getAttributeName());
				sb.append(SEPARATOR);
			}
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(END_OF_LINE);
		return sb.toString();
	}

	private String getObjectType(Class<E> clazz) {
		if (UserFields.class.getName().equals(clazz.getName()))
			return "USER";
		return "";
	}

	/*
	 * from csv header
	 */
	private String mergeValues(String[] fields) {
		if (fields == null)
			return "";
		StringBuilder sb = new StringBuilder();
		for (String a : fields) {
			sb.append(a);
			sb.append(SEPARATOR);
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(END_OF_LINE);
		return sb.toString();
	}

	/**
	 * get file Name. File name was generated using ManagedSysId
	 * 
	 * @param mngSys
	 * @return
	 */
	public String getFileName(ManagedSys mngSys, CSVSource source) {
		StringBuilder sb = new StringBuilder(pathToCSV);
		if (CSVSource.UPLOADED.equals(source)) {
			sb.append("recon_");
		}
		sb.append(mngSys.getResourceId());
		sb.append(".csv");
		return sb.toString();
	}

	/**
	 * validate fileds in csv header and value in List<AttributeMap>
	 * 
	 * @param attrMap
	 * @param headerFields
	 * @return
	 */
	private boolean validateCSVHeader(List<AttributeMap> attrMap,
			String[] headerFields, Class<E> clazz) {
		String header = mergeValues(headerFields).toLowerCase();
		for (AttributeMap am : attrMap) {
			if (!header.contains(am.getAttributeName().toLowerCase())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * generate Map <headerValue, value>
	 * 
	 * @param header
	 * @param object
	 * @return
	 */
	private Map<String, String> generataPairs(String[] header, String[] object) {
		Map<String, String> pairs = new HashMap<String, String>(0);
		for (int i = 0; i < header.length; i++) {
			pairs.put(header[i], object[i]);
		}
		return pairs;
	}

	/**
	 * Parse obj to CSV
	 * 
	 * @param attrMap
	 * @param obj
	 * @param clazz
	 * @return
	 */
	private String[] objectToCSV(List<AttributeMap> attrMap,
			ReconciliationObject<T> obj, Class<E> clazz) {
		List<String> values = new ArrayList<String>(0);

		for (String field : this.generateHeader(attrMap, clazz)
				.replace(String.valueOf(END_OF_LINE), "")
				.split(String.valueOf(SEPARATOR))) {
			AttributeMap am = this.findAttributeMapByAttributeName(attrMap,
					field);
			if (am != null) {
				if (StringUtils.hasText(am.getMapForObjectType())) {
					if (this.getObjectType(clazz).equals(
							am.getMapForObjectType())) {
						E fields;
						try {
							fields = Enum.valueOf(clazz, am
									.getAttributePolicy().getName());
						} catch (IllegalArgumentException illegalArgumentException) {
							log.info(illegalArgumentException.getMessage());
							fields = Enum.valueOf(clazz, "DEFAULT");
						}
						values.add(this.putValueIntoString(obj.getObject(),
								fields));
					} else if (PRINCIPAL_OBJECT
							.equals(am.getMapForObjectType())) {
						values.add(obj.getPrincipal() == null ? "" : obj
								.getPrincipal());
					}
				}
			} else
				values.add("");
		}
		return values.toArray(new String[0]);
	}

	private AttributeMap findAttributeMapByAttributeName(
			List<AttributeMap> attrMap, String field) {
		if (!StringUtils.hasText(field) && attrMap == null)
			return null;
		for (AttributeMap am : attrMap) {
			if (am.getAttributeName() != null
					&& am.getAttributeName().equalsIgnoreCase(field)) {
				return am;
			}
		}
		return null;
	}

	/**
	 * return "" when o is null
	 * 
	 * @param o
	 * @return
	 */
	protected static String toString(Object o) {
		return o == null ? "" : String.valueOf(o);
	}

	/**
	 * Parse from CSV to list of objects
	 * 
	 * @param managedSys
	 * @param attrMapList
	 * @param clazz
	 * @return
	 * @throws Exception
	 */
	protected List<ReconciliationObject<T>> getObjectList(
			ManagedSys managedSys, List<AttributeMap> attrMapList,
			Class<T> clazz, Class<E> enumClass, CSVSource source)
			throws Exception {
		List<ReconciliationObject<T>> objects = new ArrayList<ReconciliationObject<T>>(
				0);
		FileReader fr = this.getCSVFile(managedSys, attrMapList, enumClass,
				source);
		if (fr == null) {
			return objects;
		}
		CSVParser parser = new CSVParser(fr);

		String[][] fromParse = parser.getAllValues();
		if (fromParse.length > 1) {
			if (this.validateCSVHeader(attrMapList, fromParse[0], enumClass)) {
				for (int i = 1; i < fromParse.length; i++) {
					objects.add(this.csvToObject(clazz, enumClass, attrMapList,
							this.generataPairs(fromParse[0], fromParse[i])));
				}
			}
		}
		fr.close();
		return objects;
	}

	public Map<String, String> convertToMap(List<AttributeMap> attrMap,
			ReconciliationObject<T> obj, Class<E> clazz) {
		String[] values = this.objectToCSV(attrMap, obj, clazz);
		String[] header_ = this.generateHeader(attrMap, clazz)
				.replace(String.valueOf(END_OF_LINE), "")
				.split(String.valueOf(SEPARATOR));
		Map<String, String> result = new HashMap<String, String>(0);
		if (values.length != header_.length) {
			log.error("CSV internal error");
			return null;
		}
		for (int i = 0; i < header_.length; i++) {
			result.put(header_[i], values[i]);
		}
		return result;
	}

	public ReconciliationObject<T> toReconciliationObject(T pu,
			List<AttributeMap> attrMap, Class<E> enumClass) {
		ReconciliationObject<T> object = new ReconciliationObject<T>();
		object.setObject(pu);
		for (AttributeMap a : attrMap) {
			if (a.getAttributePolicy() != null
					&& a.getAttributePolicy().getName() != null) {
				if (PRINCIPAL_OBJECT.equals(a.getMapForObjectType())) {
					E fieldValue;
					try {
						fieldValue = Enum.valueOf(enumClass, a
								.getAttributePolicy().getName());
					} catch (IllegalArgumentException e) {
						log.info(e.getMessage());
						fieldValue = Enum.valueOf(enumClass, "DEFAULT");
					}
					object.setPrincipal(this.putValueIntoString(pu, fieldValue)
							.replaceFirst("^0*", ""));
				}
			}
		}
		return object;
	}

	/**
	 * Add object in CSV -file. If file not exists this method create one
	 * 
	 * @param newObject
	 *            - we add it in csv
	 * @param managedSys
	 *            - manage sysntem
	 * @param attrMapList
	 *            - list of data mapping
	 * @param clazz
	 *            - class of a Type
	 * @throws Exception
	 */
	protected void appendObjectToCSV(ReconciliationObject<T> newObject,
			ManagedSys managedSys, List<AttributeMap> attrMapList,
			Class<T> clazz, Class<E> enumClass, boolean append, CSVSource source)
			throws Exception {
		if (this.getCSVFile(managedSys, attrMapList, enumClass, source) != null) {
			String fName = this.getFileName(managedSys, source);
			FileWriter fw = new FileWriter(fName, append);
			fw.append(this.mergeValues(this.objectToCSV(attrMapList, newObject,
					enumClass)));
			fw.flush();
			fw.close();
		} else {
			throw new Exception("Can't work with CSV");
		}
	}

	protected void updateCSV(List<ReconciliationObject<T>> newObjectList,
			ManagedSys managedSys, List<AttributeMap> attrMapList,
			Class<T> clazz, Class<E> enumClass, boolean append, CSVSource source)
			throws Exception {
		if (this.getCSVFile(managedSys, attrMapList, enumClass, source) != null) {
			String fName = this.getFileName(managedSys, source);
			FileWriter fw = new FileWriter(fName, append);
			fw.append(this.generateHeader(attrMapList, enumClass));
			for (ReconciliationObject<T> t : newObjectList) {
				fw.append(this.mergeValues(this.objectToCSV(attrMapList, t,
						enumClass)));
			}
			fw.flush();
			fw.close();
		} else {
			throw new Exception("Can't work with CSV");
		}
	}
}
