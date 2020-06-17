import {Injectable} from '@angular/core';
import {Backend} from "./backend";
import {Observable, of} from "rxjs";
import {AveragePrices} from "./average-prices";
import {MetalIndex} from "./metal-index";
import {Accounts} from "./accounts";
import {Results} from "./results";
import {delay} from "rxjs/operators";
import {User} from "../user-login/user";

@Injectable({
    providedIn: 'root'
})
export class DummyBackendService extends Backend {

    SHORT_DELAY: number = 500;
    LONG_DELAY: number = 3000;

    calculateInterests(region: string,
                       accounts: Accounts,
                       interestRates: MetalIndex,
                       averagePrices: AveragePrices): Observable<Results> {
        return of({
                "JM REFINING UK@PMM UK-ROY": [
                    {
                        account            : "JM REFINING UK@PMM UK-ROY",
                        metal              : "XAU",
                        unit               : "tOz",
                        currency           : "GBP",
                        averageBalanceInTOz: 100.00,
                        averagePrice       : 100.00,
                        interestRate       : 0.01,
                        value              : -200.00
                    },
                    {
                        account            : "JM REFINING UK@PMM UK-ROY/ING",
                        metal              : "XPT",
                        unit               : "tOz",
                        currency           : "GBP",
                        averageBalanceInTOz: 100.00,
                        averagePrice       : 100.00,
                        interestRate       : 0.01,
                        value              : -200.00
                    },
                    {
                        account            : "JM REFINING UK@PMM UK-ROY/ING",
                        metal              : "XAU",
                        unit               : "tOz",
                        currency           : "GBP",
                        averageBalanceInTOz: 100.00,
                        averagePrice       : 100.00,
                        interestRate       : 0.01,
                        value              : -200.00
                    },
                    {
                        account            : "JM REFINING UK@PMM UK-ROY/GRA",
                        metal              : "XAU",
                        unit               : "tOz",
                        currency           : "GBP",
                        averageBalanceInTOz: 100.00,
                        averagePrice       : 100.00,
                        interestRate       : 0.01,
                        value              : -200.00
                    }],
                "UK-US Borrowings"         : [
                    {
                        metal              : "XPT",
                        unit               : "tOz",
                        currency           : "GBP",
                        averageBalanceInTOz: 100.00,
                        averagePrice       : 100.00,
                        interestRate       : 0.01,
                        value              : -200.00
                    },
                    {
                        metal              : "XPD",
                        unit               : "tOz",
                        currency           : "GBP",
                        averageBalanceInTOz: 100.00,
                        averagePrice       : 100.00,
                        interestRate       : 0.01,
                        value              : -200.00
                    },
                    {
                        metal              : "XAU",
                        unit               : "tOz",
                        currency           : "GBP",
                        averageBalanceInTOz: 100.00,
                        averagePrice       : 100.00,
                        interestRate       : 0.01,
                        value              : -200.00
                    }
                ]
            }
        ).pipe(delay(this.SHORT_DELAY));
    }

    generateDocuments(region: string,
                      interests: Results,
                      generateStatements: boolean,
                      bookCashDeals: boolean): Observable<string> {
        return of("Documents successfully generated & emailed").pipe(delay(this.LONG_DELAY));
    }

    getAccounts(): Observable<Accounts> {
        return of({
            "JM REFINING UK@PMM UK-ROY": [
                {name: "JM REFINING UK@PMM UK-ROY"},
                {name: "JM REFINING UK@PMM UK-ROY/ING"},
                {name: "JM REFINING UK@PMM UK-ROY/GRA"}],
            "UK-US Borrowings"         : [
                {name: "PMM UK@PMM US-VF"},
                {name: "INV BORROWING - UK@PMM US-VF"},
                {name: "PMM US@VALE-ACT"}]
        }).pipe(delay(this.SHORT_DELAY));
    }

    getAveragePrices(): Observable<AveragePrices> {
        return of({
            USD: {XAU: 111.11, XAG: 222.22, XPT: 333.33, XPD: 444.44},
            GBP: {XPT: 333.66, XPD: 444.88, XRH: 555.55, XIR: 666.66},
            EUR: {XRH: 555.00, XIR: 666.33, XOS: 777.77, XRU: 888.88}
        }).pipe(delay(this.SHORT_DELAY));
    }

    getInterestRates(region: string): Observable<MetalIndex> {
        return of(region === "CN" ?
                  {XAU: 0.01, XAG: 0.02, XPT: 0.03, XPD: 0.04, XRH: 0.05, XIR: 0.06, XOS: 0.07, XRU: 0.08} :
                  {XAU: 0.08, XAG: 0.07, XPT: 0.06, XPD: 0.05, XRH: 0.04, XIR: 0.03, XOS: 0.02, XRU: 0.01})
            .pipe(delay(this.SHORT_DELAY));
    }

    updateInterestRates(region: string, interestRates: MetalIndex): Observable<string> {
        return of("").pipe(delay(this.LONG_DELAY));
    }

    getUserList(): Observable<User[]> {
        return of([
            {username: "Test User1", needPassword: false},
            {username: "Test User1", needPassword: false},
            {username: "Test User3", needPassword: true}]).pipe(delay(this.SHORT_DELAY));
    }

    login(username: string, password: string): Observable<any> {
        console.log(`username: ${username}, password: ${password}`);
        return of(null).pipe(delay(this.SHORT_DELAY));
    }

    resetPassword(userName: string): Observable<any> {
        console.log(`username to update: ${userName}`);
        return of(null).pipe(delay(this.SHORT_DELAY));
    }

    isLogin(): Observable<boolean> {
        return of(true).pipe(delay(this.SHORT_DELAY));
    }
}
