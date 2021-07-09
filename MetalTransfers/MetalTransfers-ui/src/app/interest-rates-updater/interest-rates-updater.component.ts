import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {MetalIndex} from "../services/metal-index";
import {Backend} from "../services/backend";
import {AppComponent} from "../app.component";
import {finalize} from "rxjs/operators";

@Component({
    selector   : 'app-interest-rates-updater',
    templateUrl: './interest-rates-updater.component.html',
    styleUrls  : ['./interest-rates-updater.component.css', '../component-common.css']
})
export class InterestRatesUpdaterComponent implements OnInit {

    @Input() isCalculated: boolean;
    @Input() allRegions: string[];
    @Output() rateUpdateEvent = new EventEmitter<MetalIndex>();
    @Output() regionUpdateEvent = new EventEmitter<string>();

    interestRates: MetalIndex;
    region = AppComponent.DEFAULT_REGION;
    areRatesSaved: boolean = true;
    isBusy: boolean = false;
    error: string;

    constructor(private backend: Backend) {
    }

    get allMetals(): string[] {
        return Object.keys(this.interestRates).sort();
    }

    get saveStatus(): string {
        return this.areRatesSaved ?
               "The rates are saved to ENDUR" :
               "The rates are not saved to ENDUR, though the calculation will be done using the rates on the screen";
    }

    ngOnInit(): void {
        this.retrieveInterestRates(this.region);
    }

    retrieveInterestRates(region: string) {
        this.isBusy = true;
        this.backend.getInterestRates(region).pipe(finalize(() => this.isBusy = false))
            .subscribe(data => {
                this.error = undefined;
                this.interestRates = data;
                this.rateUpdateEvent.emit(this.interestRates);
                this.regionUpdateEvent.emit(this.region);
                this.areRatesSaved = true;
            }, error => {
                this.error = error;
            });
    }

    rateUpdated(metal: string, rate: string) {
        this.interestRates[metal] = Number(rate);
        this.rateUpdateEvent.emit(this.interestRates);
        this.areRatesSaved = false;
    }

    saveClicked() {
        this.isBusy = true;
        this.backend.updateInterestRates(this.region, this.interestRates)
            .pipe(finalize(() => this.isBusy = false))
            .subscribe(() => {
                this.error = undefined;
                this.areRatesSaved = true;
            }, error => {
                this.error = error;
            });
    }
}
