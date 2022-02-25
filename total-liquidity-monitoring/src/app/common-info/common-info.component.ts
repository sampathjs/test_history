import { Component, Input } from "@angular/core";
import { Group } from "./group";

@Component({
	selector: "app-common-info",
	templateUrl: "./common-info.component.html",
	styleUrls: ["./common-info.component.scss"]
})
export class CommonInfoComponent {
	@Input() currentUser?: string;
	@Input() groups: Group[] | null = [];
	@Input() balanceDates?: string[] | null = [];
	@Input() units?: string[] | null = [];

	selectedGroup?: Group;
	selectedBalanceDate?: string;
	selectedUnit?: string;
	comments?: string;
}
