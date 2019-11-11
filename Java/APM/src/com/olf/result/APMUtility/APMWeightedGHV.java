package com.olf.result.APMUtility;

import com.olf.openjvs.OException;
import com.olf.openjvs.Util;
import com.olf.openjvs.enums.IDX_UNIT_TYPE;

public class APMWeightedGHV {
		private double ghv;
		private double energyQuantity;
		private double volumeQuantity;
		private int energyUnit;
		private int volumeUnit;
		private double count;
		
		public APMWeightedGHV (int newEnergyUnit, int newVolumeUnit)
		{
			energyUnit = newEnergyUnit;
			volumeUnit = newVolumeUnit;
			
			reset();
		}
		
		public double getGHV()
		{
			return ghv;
		}
		
		public void reset()
		{
			ghv = -1.0;
			energyQuantity = 0.0;
			volumeQuantity = 0.0;
			count = 0;
		}
		
		public void calculateGHV (double newEnergyQuantity, double newVolumeQuantity, double newGhv) throws OException
		{
			if (ghv == -1.0)
			{
				energyQuantity = newEnergyQuantity;
				ghv = newGhv;
				volumeQuantity = newVolumeQuantity;
				count += 1.0;
			}
			else if (newEnergyQuantity > 0.0 && newVolumeQuantity > 0.0)
			{
				energyQuantity += newEnergyQuantity;
				volumeQuantity += newVolumeQuantity;	
				ghv = energyQuantity / volumeQuantity;
			
				count += 1.0;
			}
			else if ( (newEnergyQuantity == 0.0 && energyQuantity == 0.0) || (newVolumeQuantity == 0.0 && volumeQuantity == 0.0))
			{
				ghv = (ghv * count + newGhv) / (count + 1.0);
				count += 1.0;
			}
		}
		
		public double convertToGHV (int newEnergyUnit, int newVolumeUnit) throws OException
		{
			double euc = Util.unitconversionFactor(energyUnit, newEnergyUnit);
			double vuc = Util.unitconversionFactor(newVolumeUnit, volumeUnit);
			
			double newGhv = ghv * euc * vuc;
			
			return newGhv;
		}
		
		public double getConversionFactor (int fromUnit, int toUnit) throws OException
		{
			double uc = 1.0;
			
			if (Util.utilGetUnitTypeFromUnit(fromUnit) == IDX_UNIT_TYPE.IDX_UNIT_TYPE_ENERGY.toInt())
			{
				/* fromEnergy --> energy1 --> 1 / ghv --> volume1 --> toVolume */
				uc = 1.0 / ghv;
				double euc = Util.unitconversionFactor(fromUnit, energyUnit);
				double vuc = Util.unitconversionFactor(volumeUnit, toUnit);
				uc *= euc * vuc;
			}
			else
			{
				/* fromVolume --> volume1 --> ghv --> energy1 --> toEnergy */
				uc = ghv;
				double euc = Util.unitconversionFactor(fromUnit, volumeUnit);
				double vuc = Util.unitconversionFactor(energyUnit, toUnit);
				uc *= euc * vuc;
			}
			
			return uc;
		}
	
}
