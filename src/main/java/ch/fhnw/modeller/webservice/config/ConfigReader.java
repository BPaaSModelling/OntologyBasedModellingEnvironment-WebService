package ch.fhnw.modeller.webservice.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigReader {

	private static ConfigReader instance;

	/**
	 * Lines that start with COMMENT_PREFIX will be treated as comments and ignored.
	 */
	private static final String COMMENT_PREFIX = "#";

	private String configFilePath = "config.txt";

	private Map<String, String> configEntries;

	public ConfigReader() {

		this.configEntries = new HashMap<>();

		readConfigFile();

	}

	/**
	 * Returns the config entry with the given name.
	 * 
	 * If this value does not exist or if it could not be read from a config file
	 * then this method will return the defaultValue.
	 * 
	 * @param name         the name of the entry that should be returned
	 * @param defaultValue a default value to return if the entry does not exist
	 * @return a String with the value of the entry
	 */
	public String getEntry(String name, String defaultValue) {

		if (this.configEntries.containsKey(name)) {
			return this.configEntries.get(name);
		} else {
			System.err.println("Config file entry \"" + name + "\" could not be found. Using default value of \""
					+ defaultValue + "\"");

			return defaultValue;
		}

	}

	private void readConfigFile() {

		this.configEntries = new HashMap<>();

		File file = new File(this.configFilePath);

		try (BufferedReader br = new BufferedReader(new FileReader(file))) {

			br.lines().filter(line -> !line.isEmpty()).filter(line -> !line.startsWith(ConfigReader.COMMENT_PREFIX))
					.forEach(this::readLine);

		} catch (IOException e) {
			System.err.println();
			System.err.println("Failed to read config file \"" + configFilePath + "\":");
			e.printStackTrace();
		}
	}

	private void readLine(String line) {
		String[] splitLine = line.split("=");
		if (splitLine.length < 2) {
			System.err.println();
			System.err.println("The config file contains an invalid line: \"" + line + "\" This line will be ignored.");
			return;
		}

		String key = splitLine[0];

		String value = "";

		for (int i = 1; i < splitLine.length; i++) {
			value += splitLine[i];
		}

		if (this.configEntries.containsKey(key)) {
			System.err.println();
			System.err.println("The config file contains duplicate entries with name \"" + key
					+ "\". The second occurence will be ignored.");
			return;
		}

		this.configEntries.put(key, value);
	}

	public static ConfigReader getInstance() {

		if (instance == null) {
			instance = new ConfigReader();
		}

		return instance;

	}
}
