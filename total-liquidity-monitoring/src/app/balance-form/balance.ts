export class Balance {
	constructor(
		public customer = "",
		public currentBalance = 0,
		// public shipmentVolume = 0,
    public inUse = 0,
		public shipmentWindow?: number,
		public basisOfAssumption?: string
	) {}

	excessMetal(): number {
		// return this.currentBalance - this.shipmentVolume;
    return this.currentBalance - this.inUse;
	}

	validate(validCustomers: string[], validBasisOfAssumptions: string[]): string | undefined {
		let error = "";
		if (!validCustomers.includes(this.customer)) {
			error += `invalid customer: ${this.customer};`;
		}
		// if (!(this.currentBalance && this.shipmentVolume)) {
    if (!this.currentBalance) {

			error += "Current Balance are required";
		}
		if (this.basisOfAssumption && !validBasisOfAssumptions.includes(this.basisOfAssumption)) {
			error += `invalid basis of assumption: ${this.basisOfAssumption}`;
		}
		return error ? error : undefined;
	}
}
