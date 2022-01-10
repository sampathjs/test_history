import { Balance } from "../balance-form/balance";

export interface Forecast {
	group: string;
	balanceDate: string;
	metal: string;

	user: string;
	companyCode: string;
	unit: string;
	comments: string;
	deliverable: number;
	balances: Balance[];
}
