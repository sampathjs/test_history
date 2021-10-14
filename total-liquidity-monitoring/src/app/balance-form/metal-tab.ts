import { Balance } from "./balance";

export class MetalTab {
	metal: string;
	deliverable: number;
	balances: Balance[];

	constructor(metal: string) {
		this.metal = metal;
		this.deliverable = 0;
		this.balances = [];
	}

	totalCurrentBalance(): number | undefined {
		return this.calculateTotal((balance) => balance.currentBalance);
	}

	totalShipmentVolume(): number | undefined {
		return this.calculateTotal((balance) => balance.shipmentVolume);
	}

	totalExcessMetal(): number | undefined {
		return this.calculateTotal((balance) => balance.excessMetal());
	}

	calculateTotal(fn: (balance: Balance) => number): number | undefined {
		return this.balances.reduce((accum, curr) => accum + fn(curr), 0);
	}

	isEmpty(): boolean {
		return !this.balances.length;
	}

	clear(): void {
		this.balances = [];
	}

	validate(validCustomers: string[], validBasisOfAssumptions: string[]): string | undefined {
		let allErrors = "";
		for (let idx = 0; idx < this.balances.length; idx++) {
			const error = this.balances[idx].validate(validCustomers, validBasisOfAssumptions);
			if (error) {
				allErrors += `row ${idx + 1} has issues: ${error}<br>`;
			}
		}
		return allErrors ? allErrors : undefined;
	}
}
