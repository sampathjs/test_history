import { Component, EventEmitter, Input, Output } from "@angular/core";
import { Balance } from "./balance";
import { MatTable } from "@angular/material/table";
import { MatTabChangeEvent } from "@angular/material/tabs";
import { MetalTab } from "./metal-tab";
import { NgForm } from "@angular/forms";
import { Forecast } from "../services/forecast";

@Component({
	selector: "app-balance-form",
	templateUrl: "./balance-form.component.html",
	styleUrls: ["./balance-form.component.scss"]
})
export class BalanceFormComponent {
	@Input() existingForecasts: Forecast[] | null = [];
	@Input() customers: string[] | null = [];
	@Input() basisOfAssumptions: string[] | null = [];
	private _metals?: string[] | null;
	@Input() get metals(): string[] | null | undefined {
		return this._metals;
	}
	set metals(metals: string[] | null | undefined) {
		this._metals = metals;
		if (metals) {
			this.switchMetalTab(metals[0]);
		}
	}

	@Input() currentUser?: string;
	@Input() unit?: string;
	@Input() balanceDate?: string;
	private _group?: string;
	@Input() get group(): string | undefined {
		return this._group;
	}
	set group(group: string | undefined) {
		this._group = group;
		if (this.selectedMetalTab && this.selectedMetalTab.balances.length == 0) {
			this.setBalancesFromExistingForecasts(this.selectedMetalTab);
		}
	}

	metalTabs: Map<string, MetalTab> = new Map<string, MetalTab>();
	selectedMetalTab?: MetalTab;

	readonly columns = [
		"customer",
		"currentBalance",
		// "shipmentVolume",
    "inUse",
		"shipmentWindow",
		"basisOfAssumption",
		"excessMetal",
		"action"
	];

	@Output() saveEvent = new EventEmitter<MetalTab>();
	@Output() errorEvent = new EventEmitter<string>();

	changeTab(event: MatTabChangeEvent): void {
		if (event?.tab?.textLabel) {
			this.switchMetalTab(event.tab.textLabel);
		}
	}

	private switchMetalTab(metal: string): void {
		const metalTab = this.metalTabs.get(metal) ?? this.createMetalTab(metal);
		this.metalTabs.set(metal, metalTab);
		this.selectedMetalTab = metalTab;
	}

	private createMetalTab(metal: string): MetalTab {
		const metalTab = new MetalTab(metal);
		this.setBalancesFromExistingForecasts(metalTab);
		return metalTab;
	}

	private setBalancesFromExistingForecasts(metalTab: MetalTab): void {
		if (this.existingForecasts && this.existingForecasts.length > 0) {
			const forecasts = this.existingForecasts
				.filter((forecast) => forecast.group == this.group && forecast.metal == metalTab.metal)
				.sort()
				.reverse();
			if (forecasts.length > 0) {
				metalTab.balances = forecasts[0].balances.map((balance) => {
					const newBalance = new Balance(metalTab.metal);
					newBalance.customer = balance.customer;
					return newBalance;
				});
			}
		}
	}

	isSaveDisabled(metalTabForm: NgForm): boolean {
		return (
			metalTabForm.pristine || this.selectedMetalTab?.isEmpty() || !this.unit || !this.group || !this.balanceDate
		);
	}

	save(metalTabForm: NgForm): void {
		const error = this.selectedMetalTab?.validate(this.customers ?? [], this.basisOfAssumptions ?? []);
		if (error) {
			this.errorEvent.emit(error);
		} else {
			this.saveEvent.emit(this.selectedMetalTab);
			metalTabForm.form.markAsPristine();
		}
	}

	delete(idx: number, balanceTable: MatTable<Balance>): void {
		this.selectedMetalTab?.balances.splice(idx, 1);
		balanceTable.renderRows();
	}

	add(balanceTable: MatTable<Balance>): void {
		this.selectedMetalTab?.balances.push(new Balance());
		balanceTable.renderRows();
	}

	paste(balanceTable: MatTable<Balance>, metalTabForm: NgForm): void {
		navigator.clipboard
			.readText()
			.then((data) => {
				if (!data) {
					throw new Error("no available data from the clipboard");
				}
				if (this.selectedMetalTab) {
					this.selectedMetalTab.balances = data
						.split("\r\n")
						.filter((line) => line.length > 0)
						.map((line) => {
							const cells = line.split("\t");
							const balance = new Balance(
								cells[0],
								parseInt(cells[1]),
								parseInt(cells[2]),
								parseInt(cells[3]),
								cells[4]
							);
							const error = balance.validate(this.customers ?? [], this.basisOfAssumptions ?? []);
							if (error) {
								throw new Error(
									`invalid data row from the clipboard:<br><br>${line};<br>error:<br>${error}`
								);
							} else {
								return balance;
							}
						});
					metalTabForm.form.markAsDirty();
					balanceTable.renderRows();
				}
			})
			.catch((error: Error) => {
				this.errorEvent.emit(error.message);
			});
	}

	validate(): string | undefined {
		return this.customers && this.basisOfAssumptions && this.selectedMetalTab
			? this.selectedMetalTab.validate(this.customers, this.basisOfAssumptions)
			: undefined;
	}

	doesForecastExist(): boolean {
		return this.existingForecasts && this.existingForecasts.length > 0
			? this.existingForecasts.filter(
					(forecast) =>
						forecast.group == this.group &&
						forecast.balanceDate == this.balanceDate &&
						forecast.metal == this.selectedMetalTab?.metal
			  ).length > 0
			: false;
	}

	overwriteWarning(): string {
		return this.group && this.balanceDate && this.selectedMetalTab
			? `Forecast for group "${this.group}" at ${this.balanceDate} for metal "${this.selectedMetalTab.metal}" already exists`
			: "";
	}
}
