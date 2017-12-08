package ch.fhnw.modeller.webservice.ontology;

public class FormatConverter {

	public static int ParseOntologyInteger(String ontologyInteger){
		String[] arraySplittate = ontologyInteger.split("\\^\\^");
		if (arraySplittate.length==2){
			return Integer.parseInt(arraySplittate[0]);
		}else{
			System.out.println("===  Possible error with INTEGER "+ontologyInteger+" ===");
			return Integer.parseInt(ontologyInteger);
		}
	}
	
	public static boolean ParseOntologyBoolean(String ontologyBoolean){
		String[] arraySplittate = ontologyBoolean.split("\\^\\^");
		if (arraySplittate.length==2){
			return Boolean.parseBoolean(arraySplittate[0]);
		}else{
			System.out.println("===  Possible error with BOOLEAN "+ontologyBoolean+" ===");
			return Boolean.parseBoolean(ontologyBoolean);
		}
	}
}
