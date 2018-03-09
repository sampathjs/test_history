package com.olf.jm.interfaces.lims.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/*
 * History:
 * 2015-11-25	jwaechter	V1.0	- initial version
 * 2016-02-16	jwaechter	V1.1	- added sorting to getMeasureTypesHavingCompetingDetails()
 *                                  - changed return type of method to List<String>
 * 2016-04-12	jwaechter	V1.2	- Added changes for CR about making LOI and LOR 
 *                                    managed manually in case of LIMS based measures.
 */

/**
 * Class containing measures enriched with a source information. 
 * Basically implements a multi set for measure types.
 * @author jwaechter
 * @version 1.2
 */
public class MeasuresWithSource implements Comparable<MeasuresWithSource>{
	private final String batchNum;
	private final String purity;
	private final String brand;
	private final Map<String, List<Pair<MeasureDetails, MeasureSource>>> measuresWithSources;
	
	public MeasuresWithSource (String batchNum, String purity, String brand) {
		this.batchNum = batchNum;
		this.purity = purity;
		this.brand = brand;
		this.measuresWithSources = new TreeMap<> ();
	}
	
	public void addMeasure (MeasureDetails details, MeasureSource source) {
		String measureType = details.getMeasureType();
		
		if (measureType.equals("")) {
			throw new RuntimeException ("No measure type provided."); // should happen during development only
		}
		
		List<Pair<MeasureDetails, MeasureSource>> measuresForMeasureType;
		if (measuresWithSources.containsKey(measureType)) {
			measuresForMeasureType = measuresWithSources.get(measureType);
		} else {
			measuresForMeasureType = new ArrayList<> ();
			measuresWithSources.put(measureType, measuresForMeasureType);
		}
		measuresForMeasureType.add(new Pair<>(details, source));
	}
	
	/**
	 * Add all measures from right if right has same batch num and purity
	 * @param right
	 */
	public void addAllMeasures (MeasuresWithSource right) {
		if (right.batchNum.equals(batchNum) && right.purity.equals(purity)) {
			for (String measureType : measuresWithSources.keySet()) {
				List<Pair<MeasureDetails, MeasureSource>> alternatives = measuresWithSources.get(measureType);
				if (right.getUsedMeasureTypes().contains(measureType)) {
					alternatives.addAll(right.getMeasures(measureType));					
				}
			}
			for (String measureType : right.getUsedMeasureTypes()) {
				List<Pair<MeasureDetails, MeasureSource>> alternatives = right.getMeasures(measureType);
				if (!this.getUsedMeasureTypes().contains(measureType)) {
					
					this.measuresWithSources.put(measureType, alternatives);
				}
			}
		}
	}
	
	public List<Pair<MeasureDetails, MeasureSource>> getMeasures(String measureType) {
		return Collections.unmodifiableList(measuresWithSources.get(measureType));
	}
		
	public void removeMeasures (String measureType) {
		measuresWithSources.remove(measureType);
	}
	
	public Set<String> getUsedMeasureTypes () {
		return new HashSet<>(measuresWithSources.keySet());
	}
	
	public boolean hasCompetingMeasures() {
		for (String measureType : measuresWithSources.keySet()) {
			List<Pair<MeasureDetails, MeasureSource>> details = measuresWithSources.get(measureType);
			if (details.size() > 1) {
				return true;
			}
		}
		return false;
	}
	
	public void distinct (String measureType) {
		List<Pair<MeasureDetails, MeasureSource>> measures = measuresWithSources.get(measureType);
		if (measures.size() <= 1) {
			return;
		}
		for (int i=measures.size()-1; i >= 1; i--) {
			MeasureDetails measure1 = measures.get(i).getLeft();
			boolean remove = false;
			for (int k = i-1; k >= 0; k--) {
				MeasureDetails measure2 = measures.get(k).getLeft();
				if (measure2.equalsRequiredFields(measure1)) {
					remove = true;
					break;
				}
			}
			if (remove) {
				measures.remove(i);
			}
		}
	}
	
	
	public List<String> getMeasureTypesHavingCompetingDetails () {
		List<String> measureTypesHavingCompetingDetails = new ArrayList<>();
		for (String measureType : measuresWithSources.keySet()) {
			List<Pair<MeasureDetails, MeasureSource>> details = measuresWithSources.get(measureType);
			if (details.size() > 1) {
				measureTypesHavingCompetingDetails.add(measureType);
			}
		}
		if (measureTypesHavingCompetingDetails.size() > 0) {
			Collections.sort(measureTypesHavingCompetingDetails);			
		}
		return measureTypesHavingCompetingDetails;
	}
	
	
	public int size () {
		return measuresWithSources.size();
	}

	public String getBatchNum() {
		return batchNum;
	}

	public String getPurity() {
		return purity;
	}
	
	

	public String getBrand() {
		return brand;
	}

	public void clear() {
		measuresWithSources.clear();
	}	

	public void clear(String competingMT) {
		measuresWithSources.get(competingMT).clear();
		measuresWithSources.remove(competingMT);
	}	
	
	

	
	@Override
	public String toString() {
		return "MeasuresWithSource [batchNum=" + batchNum + ", purity="
				+ purity + ", brand=" + brand + ", measuresWithSources="
				+ measuresWithSources + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((batchNum == null) ? 0 : batchNum.hashCode());
		result = prime * result + ((brand == null) ? 0 : brand.hashCode());
		result = prime * result + ((purity == null) ? 0 : purity.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MeasuresWithSource other = (MeasuresWithSource) obj;
		if (batchNum == null) {
			if (other.batchNum != null)
				return false;
		} else if (!batchNum.equals(other.batchNum))
			return false;
		if (brand == null) {
			if (other.brand != null)
				return false;
		} else if (!brand.equals(other.brand))
			return false;
		if (purity == null) {
			if (other.purity != null)
				return false;
		} else if (!purity.equals(other.purity))
			return false;
		return true;
	}

	@Override
	public int compareTo(MeasuresWithSource arg0) {
		int diff = arg0.getBatchNum().compareTo(batchNum);
		if (diff == 0) {
			diff = arg0.getPurity().compareTo(purity);
		}
		if (diff == 0) {
			diff = arg0.getBrand().compareTo(brand);			
		}
		return diff;
	}

	public Set<String> getCompetingSampleIds() {
		Set<String> distinctSampleIds = new HashSet<String>();
		for (String measureType : measuresWithSources.keySet()) {
			List<Pair<MeasureDetails, MeasureSource>> details = measuresWithSources.get(measureType);
			for (Pair<MeasureDetails, MeasureSource> detail : details) {
				distinctSampleIds.add(detail.getLeft().getSampleId());
			}
		}
		return distinctSampleIds;
	}
	
	public boolean hasCompetingSampleIds() {
		Set<String> distinctSampleIds = new HashSet<String>();
		for (String measureType : measuresWithSources.keySet()) {
			List<Pair<MeasureDetails, MeasureSource>> details = measuresWithSources.get(measureType);
			for (Pair<MeasureDetails, MeasureSource> detail : details) {
				distinctSampleIds.add(detail.getLeft().getSampleId());
			}
			if (distinctSampleIds.size() > 1) {
				return true;
			}
		}
		return false;
	}

	public String getUniqueSampleId() {
		Set<String> distinctSampleIds = getCompetingSampleIds();
		if (distinctSampleIds.size() > 1) {
			throw new RuntimeException ("Can't retrieve unique sample ID because there are competing sample IDs");
		}
		for (String sampleId : distinctSampleIds) {
			return sampleId;
		}
		return "";
	}

	public Set<String> getAllMeasures() {
		return new HashSet<String>(measuresWithSources.keySet());
	}

	/**
	 * Takes a comma separated list of names of measure types that should be removed from this 
	 * instance.
	 * @param comSepList
	 */
	public void removeMeasureTypes(String comSepList) {
		for (String token : comSepList.split(",")) {
			String mt = token.trim();
			if (measuresWithSources.containsKey(mt)) {
				measuresWithSources.remove(mt);
			}
		}
	}

	public void removeMeasureTypesExcept(String comSepList) {
		Set<String> measuresToKeep = new HashSet<>();
		for (String token : comSepList.split(",")) {
			String mt = token.trim();
			measuresToKeep.add(mt);
		}
		for (String mt : new HashSet<String>(measuresWithSources.keySet())) {
			if (!measuresToKeep.contains(mt)) {
				measuresWithSources.remove(mt);
			}
		}
	}
}
