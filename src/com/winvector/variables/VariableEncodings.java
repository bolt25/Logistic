package com.winvector.variables;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import com.winvector.util.BurstMap;
import com.winvector.util.CountMap;

public final class VariableEncodings implements Serializable {
	private static final long serialVersionUID = 1L;
	
	// definitional 
	private final PrimaVariableInfo def;
	private final boolean useIntercept;
	// derived inputs
	public final int vdim;
	public final ArrayList<VariableMapping> adaptions = new ArrayList<VariableMapping>();
	// derived results
	public final SortedMap<String,Integer> outcomeCategories = new TreeMap<String,Integer>();
	private final ArrayList<String> outcomeNames = new ArrayList<String>();
	
	public VariableEncodings(final PrimaVariableInfo def, final boolean useIntercept,
			final Map<String,Map<String,double[]>> vectorEncodings) {
		this.def = def;
		this.useIntercept = useIntercept;
		// encode variables
		int adapterDim = 0;
		if(useIntercept) {
			final VariableMapping adaption = new ConstRecord(adapterDim);
			adaptions.add(adaption);
			adapterDim += adaption.indexR() - adaption.indexL();
		}
		for(final String ni: new TreeSet<String>(def.numericColumnSet.keySet())) {
			final VariableMapping adaption = new NumericVariable(ni,adapterDim);
			adaptions.add(adaption);
			adapterDim += adaption.indexR() - adaption.indexL();
		}
		//final Random rand = new Random(15135);
		for(final String ci: new TreeSet<String>(def.catLevels.keySet())) {
			final VariableMapping adaption;
			if((vectorEncodings!=null)&&(vectorEncodings.containsKey(ci))) {
				adaption = new LevelVectors(ci,adapterDim,vectorEncodings.get(ci));
			} else {
				final CountMap<String> levels = def.catLevels.get(ci);
				adaption = new LevelIndicators(ci,adapterDim,levels.keySet());
			}
			adaptions.add(adaption);
			adapterDim += adaption.indexR() - adaption.indexL();
		}
		vdim = adapterDim;
		// encode outcome
		for(final String oci: new TreeSet<String>(def.outcomes.keySet())) {
			outcomeCategories.put(oci,outcomeCategories.size());
			outcomeNames.add(oci);
		}
	}
	
	public PrimaVariableInfo def() {
		return def;
	}

	public boolean useIntercept() {
		return useIntercept;
	}

	public int dim() {
		return vdim;
	}
	
	public SortedMap<Integer,Double> vector(final BurstMap row) {
		final SortedMap<Integer,Double> vec = new TreeMap<Integer,Double>(); 
		for(final VariableMapping adaption: adaptions) {
			adaption.process(row,vec);
		}
		return vec;
	}
	
	public Map<String[],Double> decodeSolution(final double[] x, final boolean writeName) {
		final Map<String[],Double> res = new TreeMap<String[],Double>(CountMap.strACmp);
		for(final Map.Entry<String,Integer> mc: outcomeCategories.entrySet()) {
			final String outcome = mc.getKey();
			final int cati = mc.getValue();
			final int base = cati*vdim;
			for(final VariableMapping adaption: adaptions) {
				final SortedMap<String,Double> effects = adaption.effects(base,x);
				for(final Map.Entry<String,Double> ei: effects.entrySet()) {
					final String level = ei.getKey();
					final double effect = ei.getValue();
					//b.append(outcome + sep + cati + sep + adaption.origColumn() + sep + adaption.name() + sep + level + sep + effect + "\n");
					res.put(new String[] {outcome,""+cati,adaption.origColumn(),writeName?adaption.name():"",level},effect);
				}
			}
		}
		return res;
	}
	
	public String formatSoln(final double[] x) {
		final StringBuilder b = new StringBuilder();
		final String sep = "\t";
		b.append("outcome" + sep + "outcomegroup" + sep + "variable" + sep + "kind" + sep + "level" + sep + "value" + "\n");
		for(final Map.Entry<String,Integer> mc: outcomeCategories.entrySet()) {
			final String outcome = mc.getKey();
			final int cati = mc.getValue();
			final int base = cati*vdim;
			for(final VariableMapping adaption: adaptions) {
				final SortedMap<String,Double> effects = adaption.effects(base,x);
				for(final Map.Entry<String,Double> ei: effects.entrySet()) {
					final String level = ei.getKey();
					final double effect = ei.getValue();
					b.append(outcome + sep + cati + sep + adaption.origColumn() + sep + adaption.name() + sep + level + sep + effect + "\n");
				}
			}
		}
		return b.toString();
	}

	public String outcome(final int i) {
		return outcomeNames.get(i);
	}
	
	public int noutcomes() {
		return outcomeCategories.size();
	}

	public Integer category(final String s) {
		return outcomeCategories.get(s);
	}
}