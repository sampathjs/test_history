import { Component, OnInit, ViewChild } from "@angular/core";
import { DataService } from "./services/data-service";
import { User } from "./services/user";
import { Observable } from "rxjs";
import { Group } from "./common-info/group";
import { CommonInfoComponent } from "./common-info/common-info.component";
import { MetalTab } from "./balance-form/metal-tab";
import { Forecast } from "./services/forecast";
import { MatDialog } from "@angular/material/dialog";
import { ErrorIndicatorComponent } from "./error-indicator/error-indicator.component";

@Component({
	selector: "app-root",
	templateUrl: "./app.component.html",
	styleUrls: ["./app.component.scss"]
})
export class AppComponent implements OnInit {
	title = "total-liquidity-monitoring";

	currentUser?: User;
	users?: Observable<User[]>;
	groups?: Observable<Group[]>;
	balanceDates?: Observable<string[]>;
	units?: Observable<string[]>;
	metals?: Observable<string[]>;
	customers?: Observable<string[]>;
	basisOfAssumptions?: Observable<string[]>;
	existingForecasts?: Observable<Forecast[]>;

	loginUserChecked = false;

	@ViewChild(CommonInfoComponent)
	private commonInfoComponent?: CommonInfoComponent;

	constructor(public dataService: DataService, private dialog: MatDialog) {}

	ngOnInit(): void {
		this.dataService.login().subscribe(this.loadData);
		this.dataService.errorEmitter.subscribe(this.showError);
	}

	private loadData = (user: User) => {
		this.currentUser = user;
		this.loginUserChecked = true;
		if (user?.name) {
			this.metals = this.dataService.getMetals();
			this.basisOfAssumptions = this.dataService.getBasisOfAssumptions();
			this.balanceDates = this.dataService.getBalanceDates();
			this.units = this.dataService.getUnits();
			this.groups = this.dataService.getGroups();
			this.customers = this.dataService.getCustomers();
			this.existingForecasts = this.dataService.getForecasts();
		} else {
			this.users = this.dataService.getUsers();
		}
	};

	login(credentials: User): void {
		this.dataService.login(credentials).subscribe(this.loadData);
	}

	save(metalBalance: MetalTab): void {
		if (
			this.currentUser &&
			this.commonInfoComponent?.selectedGroup &&
			this.commonInfoComponent.selectedBalanceDate &&
			this.commonInfoComponent?.selectedUnit
		) {
			const forecast: Forecast = {
				user: this.currentUser.name,
				group: this.commonInfoComponent?.selectedGroup.name,
				companyCode: this.commonInfoComponent.selectedGroup.companyCode,
				balanceDate: this.commonInfoComponent.selectedBalanceDate,
				unit: this.commonInfoComponent.selectedUnit,
				comments: this.commonInfoComponent.comments ?? "",
				metal: metalBalance.metal,
				deliverable: metalBalance.deliverable,
				balances: metalBalance.balances
			};
			this.dataService.saveForecast(forecast).subscribe();
		}
	}

	showError = (error: string): void => {
		this.dialog.open(ErrorIndicatorComponent, { data: error });
	};
}
