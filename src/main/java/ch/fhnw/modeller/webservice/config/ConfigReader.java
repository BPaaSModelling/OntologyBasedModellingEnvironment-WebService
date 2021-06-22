package ch.fhnw.modeller.webservice.config;

public class ConfigReader {

	private static ConfigReader instance;

	public ConfigReader() {

	}

	/**
	 * Returns the config entry with the given name.
	 * 
	 * If this value does not exist then this method will return the defaultValue.
	 * 
	 * @param name         the name of the entry that should be returned
	 * @param defaultValue a default value to return if the entry does not exist
	 * @return a String with the value of the entry
	 */
	public String getEntry(String name, String defaultValue) {

		String value = System.getenv(name);

		if (value == null) {
			System.err.println("No environment variable found with the name \"" + name + "\" using default value of \""
					+ defaultValue + "\"");
			return defaultValue;
		} else {
			return value;
		}

	}

	public static ConfigReader getInstance() {

		if (instance == null) {
			instance = new ConfigReader();
		}

		return instance;

	}
}
