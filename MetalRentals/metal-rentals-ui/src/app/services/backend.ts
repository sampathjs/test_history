import {Observable} from "rxjs";
import {MetalIndex} from "./metal-index";
import {AveragePrices} from "./average-prices";
import {Accounts} from "./accounts";
import {Results} from "./results";
import {User} from "../user-login/user";

export abstract class Backend {

    abstract login(username: string, password: string): Observable<any>;

    abstract isLogin(): Observable<boolean>;

    abstract getUserList(): Observable<User[]>;

    abstract resetPassword(userName: string): Observable<any>;

    abstract getInterestRates(region: string): Observable<MetalIndex>;

    abstract updateInterestRates(region: string, interestRates: MetalIndex): Observable<string>;

    abstract getAveragePrices(region: string): Observable<AveragePrices>;

    abstract getAccounts(region: string): Observable<Accounts>;

    abstract calculateInterests(region: string,
                                accounts: Accounts,
                                interestRates: MetalIndex,
                                averagePrices: AveragePrices): Observable<Results>;

    abstract generateDocuments(region: string,
                               interests: Results,
                               generateStatements: boolean,
                               bookCashDeals: boolean): Observable<string>;
}
