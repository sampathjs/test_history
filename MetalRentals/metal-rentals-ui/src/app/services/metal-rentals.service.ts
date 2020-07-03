import {Injectable} from '@angular/core';
import {HttpClient, HttpHeaders} from "@angular/common/http";
import {environment} from "../../environments/environment";
import {Observable} from "rxjs";
import {MetalIndex} from "./metal-index";
import {AveragePrices} from "./average-prices";
import {Backend} from "./backend";
import {Accounts} from "./accounts";
import {Results} from "./results";
import {User} from "../user-login/user";
import {map} from "rxjs/operators";

@Injectable({
    providedIn: 'root'
})
export class MetalRentalsService extends Backend {

    serviceUrl: string = environment.serviceURL;

    constructor(private http: HttpClient) {
        super();
    }

    login(username: string, password: string): Observable<any> {
        const headers = new HttpHeaders({authorization: 'Basic ' + btoa(username + ':' + password)});
        return this.http.get<boolean>(`${this.serviceUrl}/users/current`,
            {headers: headers});
    }

    isLogin(): Observable<boolean> {
        return this.http.get<boolean>(`${this.serviceUrl}/users/current`).pipe(map(user => user != null));
    }

    getUserList(): Observable<User[]> {
        return this.http.get<User[]>(`${this.serviceUrl}/users`);
    }

    resetPassword(userName: string): Observable<any> {
        return this.http.post<User[]>(`${this.serviceUrl}/users/${userName}/password`, {});
    }

    getInterestRates(region: string): Observable<MetalIndex> {
        return this.http.get<MetalIndex>(`${this.serviceUrl}/interest_rates/${region}`);
    }

    updateInterestRates(region: string, interestRates: MetalIndex): Observable<string> {
        return this.http.put<string>(`${this.serviceUrl}/interest_rates/${region}`, interestRates);
    }

    getAveragePrices(region: string): Observable<AveragePrices> {
        return this.http.get<AveragePrices>(`${this.serviceUrl}/average_prices/${region}`);
    }

    getAccounts(region: string): Observable<Accounts> {
        return this.http.get<Accounts>(`${this.serviceUrl}/accounts/${region}`);
    }

    calculateInterests(region: string,
                       accounts: Accounts,
                       interestRates: MetalIndex,
                       averagePrices: AveragePrices): Observable<Results> {
        const body: {} = {accounts: accounts, interestRates: interestRates, averagePrices: averagePrices};
        return this.http.put<Results>(`${this.serviceUrl}/interests/${region}`, body);
    }

    generateDocuments(region: string,
                      interests: Results,
                      generateStatements: boolean,
                      bookCashDeals: boolean): Observable<string> {
        return this.http.put<string>(`${this.serviceUrl}/documents/${region}?statements=${generateStatements}&&deals=${bookCashDeals}`,
            interests);
    }
}
