/*
 * Description:
 * 
 * StockPosition class is used to create a data structure using returnT as input and output a map like below:
 * Key - balance Line name (reading from user_const_repo
 * Value - List of StockPosition objects
 * each object has two string values and one list (list of metals, Key -Metal Name, value - Metal value
 * ------------------------------------------------------------------------------------------------------------------------------
 * Key - BalanceLine|	Value – List of StockPosition objects
 * ------------------------------------------------------------------------------------------------------------------------------
	L140 Stock UK	|	L140 Stock UK					|	L140 Stock UK					|	L140 Stock UK
					|	Sponge							|	Grain							|	Ingot
					|	Pt	Pd	Rh	Ir	Ru	Os	Au	Ag	|	Pt	Pd	Rh	Ir	Ru	Os	Au	Ag	|	Pt	Pd	Rh	Ir	Ru	Os	Au	Ag
					|	1.2 2.7 3.4	0.0 7.5 6.5 6.2 0.0	|	7.5 6.5 6.2 0.0 1.2 2.7 3.4	0.0 |	6.5 6.2 0.0 1.2 2.7 3.4 2.1 0.0
	-------------------------------------------------------------------------------------------------------------------------------		
	L145 Stock US	|	L145 Stock US					|	L145 Stock US					|	L145 Stock US
					|	Sponge							|	Grain							|	Ingot
					|	Pt	Pd	Rh	Ir	Ru	Os	Au	Ag	|	Pt	Pd	Rh	Ir	Ru	Os	Au	Ag	|	Pt	Pd	Rh	Ir	Ru	Os	Au	Ag
					|	1.2 2.7 3.4	0.0 7.5 6.5 6.2 0.0	|	7.5 6.5 6.2 0.0 1.2 2.7 3.4	0.0 |	6.5 6.2 0.0 1.2 2.7 3.4 2.1 0.0
	-------------------------------------------------------------------------------------------------------------------------------					
 * And so on.....
 * 
 * Stock Split by Form Report is based on Metals balance sheet project.
 * 
 * History:
 * 2020-03-18	V1.0	Jyotsna	- Initial version, Developed under SR 323601
 * 
 */
package com.jm.rbreports.BalanceSheet;

import java.util.HashMap;

public class StockPosition {
	
	private String balanceLine;
	private String formType;
	private HashMap<String,Double> metalPosition = new HashMap<>();


	//constructor
	public StockPosition(String balanceLine, String formType){
		this.balanceLine = balanceLine;
		this.formType = formType;
		this.metalPosition = new HashMap<>();
	}

	//getter
	public String getBalanceLine(){
		return balanceLine;
	}

	public String getFormType(){ 
		return formType;
	}

	public HashMap<String,Double> getMetalPosition(){
		return metalPosition;
	}

	public double getMetalPosition(String metal){
		Double aggregatedPosition = metalPosition.get(metal);
		if(aggregatedPosition == null) {
			aggregatedPosition = 0.0d;
		}
		return aggregatedPosition;
	}
	
	public void addMetalPosition(String metal, Double position) {
		Double aggregatedPosition = metalPosition.get(metal);
		if(aggregatedPosition == null) {
			aggregatedPosition = position;
		} else {
			aggregatedPosition += position;
		}
		
		metalPosition.put(metal, aggregatedPosition);
	}

}