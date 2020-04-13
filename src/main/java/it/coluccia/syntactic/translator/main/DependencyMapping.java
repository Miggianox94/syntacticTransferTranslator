package it.coluccia.syntactic.translator.main;

import java.util.List;

import simplenlg.features.Feature;
import simplenlg.features.LexicalFeature;
import simplenlg.features.Tense;
import simplenlg.framework.LexicalCategory;
import simplenlg.framework.NLGElement;
import simplenlg.phrasespec.NPPhraseSpec;
import simplenlg.phrasespec.SPhraseSpec;
import simplenlg.phrasespec.VPPhraseSpec;

/**
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
 * @author Miggianox94
 *
 */
public enum DependencyMapping {

    COP("cop"), 
    DET("det"),
    NMOD("nmod"),
    CASE("case"),
    DETPOSS("det:poss"),
    AUX("aux"),
    DOBJ("dobj"),
    AMOD("amod"),
    NSUBJPASS("nsubjpass"),
    AUXPASS("auxpass"),
    ADVMOD("advmod")
    ; 


    private final String tintDep;

    private DependencyMapping(String tintDep) {
        this.tintDep = tintDep;
    }
    
    
    
    
    
    /**
	 * The simplenlg phrases categories are:
	 * [verb,preposition,noun,adverb,adjective]phrase
	 * @param sentence
	 * @param governor
	 * @param dependencies
	 */
    public static void setSimpleNlgPos(SPhraseSpec sentence, int mainRootIndex, int governor, List<Dependency> dependencies){
    	
    	for(Dependency dep : dependencies){
    		if(dep.getDep().equals("cop")){
    			String value = Executor.itaToEnDict.get(dep.getDependentGloss());
    			sentence.setVerb(value);
    			sentence.setSubject("It");
    		}else if(dep.getDep().equals("det:poss")){
    			
				NPPhraseSpec subjNP = Executor.nlgFactory.createNounPhrase();
				subjNP = findSpecifier(dep.getGovernor(),subjNP,dependencies,dep.getGovernor());
				NLGElement word = Executor.nlgFactory.createInflectedWord(Executor.itaToEnDict.get(dep.getDependentGloss())+" "+Executor.itaToEnDict.get(dep.getGovernorGloss()), LexicalCategory.NOUN);
				word.setFeature(LexicalFeature.PROPER, true);
				NPPhraseSpec possNP = Executor.nlgFactory.createNounPhrase(word);
				possNP.setFeature(Feature.POSSESSIVE, true);
				subjNP.setSpecifier(possNP);
				
				System.out.println("Setting subj: "+subjNP);
				sentence.setComplement(subjNP);
    		}else if(dep.getDep().equals("aux")){
    			boolean auxpass = false;
    			for(Dependency dep2:dependencies){
    				if(dep2.getGovernor() == dep.getGovernor() && dep2.getDep().equals("auxpass")){
    					auxpass = true;
    	    			VPPhraseSpec verbPhrase = Executor.nlgFactory.createVerbPhrase();
    	    			verbPhrase.setVerb(Executor.itaToEnDict.get(dep.getGovernorGloss()));
    	    			for(Dependency dep3:dependencies){
    	    				if(dep3.getGovernor() == dep.getGovernor() && dep3.getDep().equals("advmod")){
    	    					verbPhrase.setPostModifier(Executor.itaToEnDict.get(dep2.getDependentGloss()));
    	    					break;
    	    				}
    	    			}
    	    			System.out.println("Setting verbphrase: "+verbPhrase);
    	    			sentence.setVerb(verbPhrase);
    	    			sentence.setFeature(Feature.TENSE, Tense.PAST);
    	    			sentence.setFeature(Feature.PASSIVE, true);
    					break;
    				}
    			}
    			if(!auxpass){
        			VPPhraseSpec verbPhrase = Executor.nlgFactory.createVerbPhrase();
        			verbPhrase.setVerb(Executor.itaToEnDict.get(dep.getGovernorGloss()));
        			System.out.println("Setting verbphrase: "+verbPhrase);
        			sentence.setVerb(verbPhrase);
        			sentence.setFeature(Feature.TENSE, Tense.PAST);
        			sentence.setSubject("He");    				
    			}

    		}else if(dep.getDep().equals("det") && dep.getGovernor() != governor){
    			NPPhraseSpec complementPhrase = Executor.nlgFactory.createNounPhrase();
    			complementPhrase.setDeterminer(Executor.itaToEnDict.get(dep.getDependentGloss()));
    			
    			boolean nmodFound = false;
    			for(Dependency dep2:dependencies){
    				if(dep2.getGovernor() == dep.getGovernor()){
    					//dep2 and dep are brothers
    					if(dep2.getDep().equals("amod")){
    						complementPhrase.setPreModifier(Executor.itaToEnDict.get(dep2.getDependentGloss()));
    					}
    					if(dep2.getDep().equals("nmod")){
    						nmodFound = true;
    						NPPhraseSpec subjNP = Executor.nlgFactory.createNounPhrase();
    						subjNP.setNoun(Executor.itaToEnDict.get(dep2.getGovernorGloss()));
    						subjNP.setPlural(true);
    					
    						NLGElement word = Executor.nlgFactory.createInflectedWord(Executor.itaToEnDict.get(dep2.getDependentGloss()), LexicalCategory.NOUN);
    						word.setFeature(LexicalFeature.PROPER, true);
    						NPPhraseSpec possNP = Executor.nlgFactory.createNounPhrase(word);
    						possNP.setFeature(Feature.POSSESSIVE, true);
    						for(Dependency dep3:dependencies){
    							if(dep3.getGovernor() == dep2.getDependent() && dep3.getDep().equals("amod")){
    								possNP.setPreModifier(Executor.itaToEnDict.get(dep3.getDependentGloss()));

    							}
    						}
    						subjNP.setSpecifier(possNP);
    						
    						complementPhrase.setComplement(subjNP);
    					}
    				}
    			}
    			
    			if(nmodFound){
        			System.out.println("Setting subject: "+complementPhrase);
        			sentence.setObject(complementPhrase);
    			}else{
    				complementPhrase.setNoun(Executor.itaToEnDict.get(dep.getGovernorGloss()));
        			System.out.println("Setting object: "+complementPhrase);
        			sentence.setObject(complementPhrase);	
    			}
    		}
    	}
    }
    
    private static NPPhraseSpec findSpecifier(int governorIndex, NPPhraseSpec constructed, List<Dependency> dependencies, int firstGov){
    	if(governorIndex == 0){
    		for(Dependency dp : dependencies){
        		if(dp.getGovernor() == 0){
        			System.out.println("Setting noun: "+dp.getDependentGloss());
        			constructed.setNoun(Executor.itaToEnDict.get(dp.getDependentGloss()));
        			return constructed;
        		}
        	}
    		return constructed;
    	}
    	
    	for(Dependency dp : dependencies){
    		if(dp.getDependent() == governorIndex){
    			NPPhraseSpec constructedPartial = findSpecifier(dp.getGovernor(),constructed,dependencies,firstGov);
    			
    			if(governorIndex == firstGov || dp.getGovernor() == 0){
    				return constructedPartial;
    			}
    			System.out.println("Adding modifier: "+dp.getDependentGloss());
    			//I assume that this will be a modifier
    			constructedPartial.addPreModifier(Executor.itaToEnDict.get(dp.getDependentGloss()));
    			return constructedPartial;
    		}
    	}
    	return constructed;
    }
    

	public String getTintDep() {
		return tintDep;
	}
    
    
}
