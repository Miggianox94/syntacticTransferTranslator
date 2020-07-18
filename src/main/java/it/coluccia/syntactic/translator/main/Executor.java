package it.coluccia.syntactic.translator.main;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.stanford.nlp.pipeline.Annotation;
import eu.fbk.dh.tint.runner.TintPipeline;
import eu.fbk.dh.tint.runner.TintRunner;
import simplenlg.framework.NLGFactory;
import simplenlg.lexicon.Lexicon;
import simplenlg.phrasespec.SPhraseSpec;
import simplenlg.realiser.english.Realiser;

public class Executor {
	
	private static TintPipeline pipeline;
	private static Lexicon    lexicon;
	public static NLGFactory nlgFactory;
	private static Realiser   realiser;
	
	public static Map<String,String> itaToEnDict;
	
	private final static String[] REQUIRED_TRANSLATIONS= new String[]{
			"È la spada laser di tuo padre",
			"Ha fatto una mossa leale",
			"Gli ultimi avanzi della vecchia Repubblica sono stati spazzati via"
			};

	public static void main(String[] args){
		try {
			init();
		} catch (IOException e) {
			System.out.println("Problems during init phase, abort!");
			return;
		}
		System.out.println("########################## INITIALIZATION PHASE COMPLETED\n\n");
		for(String phrase : REQUIRED_TRANSLATIONS){
			List<Dependency> dependencyTree;
			try {
				dependencyTree = constructDependencyTree(phrase);
				dependencyTree.forEach(d -> System.out.println(d));
				System.out.println("\n");
				SPhraseSpec simpleNLGInput = translationAndannotationToSPhraseSpec(dependencyTree);
				String translated_sentence = realiser.realiseSentence(simpleNLGInput);
				System.out.println(">>>>>>>>>> TRANSLATED SENTENCE ["+phrase+"] --> ["+translated_sentence+"]\n\n");
			} catch (IOException e) {
				System.err.println("Error during translation of sentence ["+phrase+"], the process will try with the others");
				e.printStackTrace();
			}
		}

	}
	
	private static void init() throws IOException{
		// Initialize the Tint pipeline
		pipeline = new TintPipeline();

		// Load the default properties
		// see https://github.com/dhfbk/tint/blob/master/tint-runner/src/main/resources/default-config.properties
		pipeline.loadDefaultProperties();

		// Add a custom property
		// pipeline.setProperty("my_property", "my_value");

		// Load the models
		pipeline.load();
		
		//Creating SimpleNLG objects
		lexicon = Lexicon.getDefaultLexicon();
		nlgFactory = new NLGFactory(lexicon);
		realiser = new Realiser(lexicon);
		
		//initializing dict
		itaToEnDict = new HashMap<>();
		itaToEnDict.put("È", "is");
		itaToEnDict.put("la", "the");
		itaToEnDict.put("spada", "sword");
		itaToEnDict.put("laser", "laser");
		itaToEnDict.put("di", "of");
		itaToEnDict.put("tuo", "your");
		itaToEnDict.put("padre", "father");
		itaToEnDict.put("Ha", "has");
		itaToEnDict.put("fatto", "do");
		itaToEnDict.put("una", "a");
		itaToEnDict.put("mossa", "move");
		itaToEnDict.put("leale", "loyal");
		itaToEnDict.put("Gli", "the");
		itaToEnDict.put("ultimi", "last");
		itaToEnDict.put("avanzi", "scrap");
		itaToEnDict.put("della", "of");
		itaToEnDict.put("vecchia", "old");
		itaToEnDict.put("Repubblica", "Republic");
		itaToEnDict.put("sono", "are");
		itaToEnDict.put("stati", "been");
		itaToEnDict.put("spazzati", "swept");
		itaToEnDict.put("via", "away");
		

	}
	
	private static List<Dependency> constructDependencyTree(String phrase) throws IOException{
		// Get the original Annotation (Stanford CoreNLP)
		//Annotation stanfordAnnotation = pipeline.runRaw(phrase);


		
		// (optionally getting the original Stanford CoreNLP Annotation as return value)
		InputStream stream = new ByteArrayInputStream(phrase.getBytes(StandardCharsets.UTF_8));
		OutputStream outStream = new ByteArrayOutputStream();
		@SuppressWarnings("unused")
		Annotation annotatedText = pipeline.run(stream, outStream, TintRunner.OutputFormat.JSON);
		String dependencyTree = outStream.toString();
		
		// Get the JSON
		Gson gson = new Gson();
		JsonObject body = gson.fromJson(dependencyTree, JsonObject.class);
		JsonArray sentences = body.get("sentences").getAsJsonArray();
		JsonObject firstResult = sentences.get(0).getAsJsonObject();
		JsonArray basicDeps = firstResult.get("basic-dependencies").getAsJsonArray();
		
		List<Dependency> toRet = new ArrayList<>();

		//parsing to Java POJO
		for(int i=0;i<basicDeps.size();i++){
			JsonObject dependency = basicDeps.get(i).getAsJsonObject();
			String dep = dependency.get("dep").getAsString();
			int governor = dependency.get("governor").getAsInt();
			String governorGloss = dependency.get("governorGloss").getAsString();
			int dependent = dependency.get("dependent").getAsInt();
			String dependentGloss = dependency.get("dependentGloss").getAsString();
			
			Dependency depObj = new Dependency(dep,governor,governorGloss,dependent,dependentGloss);
			toRet.add(depObj);
		}
		
		return toRet;
	}

	

	/**
	 * It translates the sentence word by word and then it maps to SPhraseSpec the Annotation object
	 * ref: https://nlp.stanford.edu/software/dependencies_manual.pdf
	 * It only covers a subset of the basic-dependencies types of Stanford NLP (the required ones from the required sentences)
	 * It maps to the SimpleNLG types: https://github.com/simplenlg/simplenlg/wiki/Section-III-%E2%80%93-Getting-started
	 * @param dependencies
	 * @return
	 */
	private static SPhraseSpec translationAndannotationToSPhraseSpec(List<Dependency> dependencies){
		/**
		 * ###### How to decode annotation structure #####
		 *     "dep":"nsubj", --> it is the dependency class
               "governor":4,  --> it is the governor index
               "governorGloss":"avevano",  --> it is the governor word
               "dependent":2,  --> it is the dependent index
               "dependentGloss":"topi" --> it is the dependent word
               
               It is the mapping(x=governorGloss;y=dependentGloss):
               ROOT -> 
               cop(x,y) -> setVerb(y)
               det(x,y) -> setDerminer(y)
               nmod(x,y) -> addModifier(y)
               case(x,y) -> addComplement(y)
               det:poss(x,y) -> setDerminer(y)
               aux(x,y) -> setVerb(y)
               dobj(x,y) -> setObject(y)
               amod(x,y) -> addModifier(y)
               nsubjpass(x,y) -> setSubject(y)
               auxpass(x,y) -> setVerb(y)
               advmod(x,y) -> addModifier(y)
		 */
		SPhraseSpec sentence = nlgFactory.createClause();

		
		//governorIndex conterrà l'index del dependent della root
		int governorIndex = -1;
		for(Dependency dep : dependencies){
			if(dep.getGovernor() == 0){
				governorIndex = dep.getDependent();
			}
		}
		final int governorIndexFinal = governorIndex;
		final List<Dependency> dependenciesFinal = dependencies;
		
		Collections.sort(dependencies, new Comparator<Dependency>(){

			@Override
			public int compare(Dependency o1, Dependency o2) {
				if(o1.getGovernor() == 0 || !isLeaf(o1,dependenciesFinal)){
					return 1;
				}else{
					return -1;//le foglie vengono dopo nell'ordinamento
				}
			}	
		});
		
		DependencyMapping.setSimpleNlgPos(sentence,governorIndexFinal,dependencies);
		
		
		
		return sentence;
	}
	
	/**
	 * return true if not exist a dependency dep where dep.getGovernor() == a.getDependent()
	 * @param a
	 * @param dependencies
	 * @return
	 */
	private static boolean isLeaf(Dependency a,List<Dependency> dependencies){
		for(Dependency dep:dependencies){
			if(dep.getGovernor() == a.getDependent()){
				return false;
			}
		}
		return true;
	}

}
