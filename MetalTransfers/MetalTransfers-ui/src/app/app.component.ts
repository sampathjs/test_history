import {Component, OnInit} from '@angular/core';
import {MetalIndex} from "./services/metal-index";
import {AveragePrices} from "./services/average-prices";
import {Results} from "./services/results";
import {Backend} from "./services/backend";
import {finalize} from "rxjs/operators";

@Component({
    selector   : 'app-root',
    templateUrl: './app.component.html',
    styleUrls  : ['./app.component.css']
})
export class AppComponent implements OnInit {
    public static REGION_NON_CN = "NonCN";
    public static REGION_CN = "CN";
    public static DEFAULT_REGION = AppComponent.REGION_NON_CN;

    isBusy: boolean = false;
    error: string;
    authenticated: boolean = false;
    region: string = AppComponent.DEFAULT_REGION;
    allRegions: string[] = [AppComponent.REGION_NON_CN, AppComponent.REGION_CN];
    interestRates: MetalIndex;
    averagePrices: AveragePrices;
    interests: Results;

    title = 'Metal Rentals';

    constructor(private backend: Backend) {
    }

    ngOnInit() {
        this.isBusy = true;
        this.backend.isLogin().pipe(finalize(() => this.isBusy = false)).subscribe(data => {
            this.authenticated = data;
            this.error = undefined;
        }, error => {
            this.error = error;
        });
    }

    updateAuthenticated(flag: boolean) {
        this.authenticated = flag;
    }

    updateRegion(region: string) {
        this.region = region;
    }

    updateInterestRates(interestRates: MetalIndex) {
        this.interestRates = interestRates;
    }

    updateAveragePrices(averagePrices: AveragePrices) {
        this.averagePrices = averagePrices;
    }

    updateInterests(interests: Results) {
        this.interests = interests;
    }
}
