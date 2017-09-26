package ch.fhnw.modeller.webservice.ontology;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class RuleParser {
	
	private static StringBuilder ruleFragment;
	private static List<String> ruleSet;

	public static List<String> parseRules(InputStream in) throws IOException {
		System.out.println("************** " +in.toString());
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.contains("#")) {
				if (!line.replace(" ", "").startsWith("#")) {
					line = line.substring(0, line.indexOf("#"));
					addLine(line);
				}
			} else if(!line.trim().equals("")){
				addLine(line);
			}
		}
		reader.close();
		return ruleSet;
	}

	private static void addLine(String line) {
		String startPattern =	"[";
		String endPattern	=	"]";
		if(line.contains(startPattern) && line.contains(endPattern)){
//			System.out.println("a " +line);
			addRuleToRuleSet(line.substring(line.indexOf(startPattern) + 1, line.indexOf(endPattern)));
		}
		else if(line.contains(startPattern) && !line.contains(endPattern)){
//			System.out.println("b " +line);
			startRule(line.substring(line.indexOf(startPattern) + 1, line.length()));
		}
		else if(!line.contains(startPattern) && line.contains(endPattern)){
//			System.out.println("c " +line);
			appendToRuleAndEnd(line.substring(0,line.indexOf(endPattern)));
		}
		else{
//			System.out.println("d " +line);
			appendToRule(line);
		}
	}

	private static void startRule(String substring) {
		ruleFragment = new StringBuilder();
		appendToRule(substring);
	}

	private static void appendToRuleAndEnd(String substring) {
		if(ruleFragment!=null){
			appendToRule(substring);
			addRuleToRuleSet(ruleFragment.toString());
			ruleFragment = null;
		}
	}

	private static void appendToRule(String substring) {
		if(ruleFragment!=null){
			ruleFragment.append(substring);
		}
	}

	private static void addRuleToRuleSet(String rule) {
		if(ruleSet==null){
			ruleSet = new ArrayList<String>();
		}
		ruleSet.add(rule.replaceAll("\\s+", " ").trim());
	}
}