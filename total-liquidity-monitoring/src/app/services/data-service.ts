import { EMPTY, Observable } from "rxjs";
import { User } from "./user";
import { HttpClient, HttpHeaders } from "@angular/common/http";
import { catchError, finalize } from "rxjs/operators";
import { Group } from "../common-info/group";
import { EventEmitter, Injectable } from "@angular/core";
import { Forecast } from "./forecast";

@Injectable({
	providedIn: "root"
})
export class DataService {
	private waitingCalls: unknown[] = [];
	readonly errorEmitter = new EventEmitter<string>();

	isBusy(): boolean {
		return this.waitingCalls.length > 0;
	}

	constructor(private http: HttpClient) {}

	login(credential?: User): Observable<User> {
		const headers = new HttpHeaders(
			credential
				? {
						authorization: "Basic " + btoa(credential.name + ":" + (credential.password ?? ""))
				  }
				: {}
		);
		return this.getData("/login_user", headers);
	}

	getUsers(): Observable<User[]> {
		return this.getData("/users");
	}

	resetPassword(userName: string): Observable<unknown> {
		this.waitingCalls.push({});
		return this.http
			.post<User[]>(`/users/${userName}/password`, {})
			.pipe(finalize(() => this.waitingCalls.pop()))
			.pipe(
				catchError((err: Error) => {
					{
						this.errorEmitter.emit(err.message);
						return EMPTY;
					}
				})
			);
	}

	getGroups(): Observable<Group[]> {
		return this.getData("/groups");
	}

	getBalanceDates(): Observable<string[]> {
		return this.getData("/balance_dates");
	}

	getUnits(): Observable<string[]> {
		return this.getData("/units");
	}

	getMetals(): Observable<string[]> {
		return this.getData("/metals");
	}

	getCustomers(): Observable<string[]> {
		return this.getData("/customers");
	}

	getBasisOfAssumptions(): Observable<string[]> {
		return this.getData("/basis_of_assumptions");
	}

	getForecasts(): Observable<Forecast[]> {
		return this.getData("/forecasts");
	}

	private getData<T>(endpoint: string, headers?: HttpHeaders): Observable<T> {
		this.waitingCalls.push({});
		return this.http
			.get<T>(endpoint, headers ? { headers } : undefined)
			.pipe(
				finalize(() => {
					this.waitingCalls.pop();
				})
			)
			.pipe(
				catchError((err: Error) => {
					{
						this.errorEmitter.emit(
							`${err.message}<br><br>Please try again or contact support with above error message`
						);
						return EMPTY;
					}
				})
			);
	}

	saveForecast(forecast: Forecast): Observable<unknown> {
		console.log(JSON.stringify(forecast));
		this.waitingCalls.push({});
		const headers = new HttpHeaders({ "Content-Type": "application/json" });
		return this.http
			.post<Forecast[]>(`/forecasts`, forecast, { headers })
			.pipe(
				finalize(() => {
					console.log("saved");
					this.waitingCalls.pop();
				})
			)
			.pipe(
				catchError((err: Error) => {
					{
						this.errorEmitter.emit(err.message);
						return EMPTY;
					}
				})
			);
	}
}
