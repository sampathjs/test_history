import {Component, EventEmitter, Input, Output} from '@angular/core';
import {AveragePrices} from "../services/average-prices";
import {Backend} from "../services/backend";
import {finalize} from "rxjs/operators";

@Component({
    selector   : 'app-average-prices-viewer',
    templateUrl: './average-prices-viewer.component.html',
    styleUrls  : ['./average-prices-viewer.component.css', '../component-common.css']
})
export class AveragePricesViewerComponent {

    @Output() pricesRetrievedEvent = new EventEmitter<AveragePrices>();

    averagePrices: AveragePrices;
    tableDataSource: object[] = [];
    columns: string[] = ["Currency"];
    isExpanded: boolean;
    isBusy: boolean = false;
    error: string;

    constructor(private backend: Backend) {
    }

    @Input()
    set region(region: string) {
        this.isBusy = true;
        this.backend.getAveragePrices(region)
            .pipe(finalize(() => this.isBusy = false))
            .subscribe(data => {
                this.error = undefined;
                this.averagePrices = data;
                this.pricesRetrievedEvent.emit(data);
                this.genTableDataSource();
            }, error => {
                this.error = error;
            });
    }

    private genTableDataSource() {
        this.tableDataSource = [];
        for (let currency of Object.keys(this.averagePrices).sort()) {
            let prices = {Currency: currency};
            for (let metal of Object.keys(this.averagePrices[currency]).sort()) {
                if (!this.columns.includes(metal)) {
                    this.columns.push(metal);
                }
                prices[metal] = this.averagePrices[currency][metal];
            }
            this.tableDataSource.push(prices);
        }
    }
}
