import {Component, EventEmitter, Input, Output} from '@angular/core';
import {MetalIndex} from "../services/metal-index";
import {AveragePrices} from "../services/average-prices";
import {Accounts} from "../services/accounts";
import {Backend} from "../services/backend";
import {Results} from "../services/results";
import {finalize} from "rxjs/operators";

@Component({
    selector   : 'app-interest-calculator',
    templateUrl: './interests-calculator.component.html',
    styleUrls  : ['./interests-calculator.component.css', '../component-common.css']
})
export class InterestsCalculatorComponent {

    @Input() interestRates: MetalIndex;
    @Input() averagePrices: AveragePrices;
    @Output() resultEmitter = new EventEmitter<Results>();
    accounts: Accounts;
    selectedAccountGroups: string[] = [];
    selectedAll: boolean = true;
    resultColumnMapping = {
        account       : "Account",
        metal         : "Metal",
        unit          : "Unit",
        currency      : "Currency",
        averageBalance: "Avg Balance",
        averagePrice  : "Avg Price",
        interestRate  : "Interest Rate",
        value         : "Interest"
    };
    isExpanded: boolean;
    isCalculated: boolean = false;
    isBusy: boolean = false;
    error: string;
    results: Results;

    constructor(private backend: Backend) {
    }

    _region: string;

    @Input()
    set region(region: string) {
        this._region = region;
        this.isBusy = true;
        this.backend.getAccounts(region).pipe(finalize(() => this.isBusy = false))
            .subscribe(data => {
                this.error = undefined;
                this.accounts = data;
                this.selectedAccountGroups = this.accountGroups;
                this.selectedAll = true;
            }, error => {
                this.error = error;
            });
    }

    get accountGroups() {
        return Object.keys(this.accounts).sort();
    }

    get resultColumns() {
        return Object.keys(this.resultColumnMapping);
    }

    get readyToCalculate(): boolean {
        return !this.isCalculated &&
               this.error ==
               undefined &&
               (this.selectedAll || this.selectedAccountGroups.length > 0);
    }

    get resultAccountGroups() {
        return this.selectedAll ? this.accountGroups.sort() : this.selectedAccountGroups.sort();
    }

    updateSelectedAccountGroups() {
        this.selectedAccountGroups = this.selectedAll ? this.accountGroups : [];
    }

    columnName(column: string): string {
        return this.resultColumnMapping[column];
    }

    calculateClicked() {
        let selectedAccounts: Accounts = {};
        if (this.selectedAll) {
            selectedAccounts = this.accounts;
        } else {
            this.selectedAccountGroups.forEach(group => selectedAccounts[group] = this.accounts[group]);
        }
        this.isBusy = true;
        this.backend.calculateInterests(this._region, selectedAccounts, this.interestRates, this.averagePrices)
            .pipe(finalize(() => this.isBusy = false))
            .subscribe(data => {
                this.error = undefined;
                this.results = data;
                this.resultEmitter.emit(data);
                this.isCalculated = true;
            }, error => {
                this.error = error;
            });
    }
}
